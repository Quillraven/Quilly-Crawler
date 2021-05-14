package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Queue
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Constructor
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.combat.command.CommandDefend
import com.github.quillraven.quillycrawler.combat.command.CommandTargetType
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.collections.GdxArray
import ktx.collections.getOrPut
import ktx.collections.isNotEmpty
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger
import kotlin.reflect.KClass

private enum class CombatPhase {
  NEW_TURN, WAIT_FOR_PLAYER_ORDER, PREPARE_COMMANDS, EXECUTE_COMMANDS
}

private class CommandPool<T : Command>(
  private val constructor: Constructor,
  private val context: CombatContext
) : Pool<T>() {
  override fun newObject(): T {
    @Suppress("UNCHECKED_CAST")
    return constructor.newInstance(context) as T
  }
}

class CombatSystem(
  private val combatContext: CombatContext,
  private val gameEventDispatcher: GameEventDispatcher,
  private val bTreeManager: BehaviorTreeLibraryManager = BehaviorTreeLibraryManager.getInstance()
) : GameEventListener, SortedIteratingSystem(
  allOf(CombatComponent::class, StatsComponent::class).exclude(RemoveComponent::class).get(),
  // Agility defines the order how entities are executed.
  // Higher agility means an entity executes its combat order faster.
  compareBy { -it.statsCmp[StatsType.AGILITY] }
) {
  private val commandQueue = Queue<Command>()
  private var combatPhase = CombatPhase.NEW_TURN
    set(value) {
      LOG.debug { "Switching to combat phase $value" }
      field = value
    }
  private val victoryEvent = CombatVictoryEvent()
  private val defeatEvent = CombatDefeatEvent()
  private val playerTurnEvent = CombatPlayerTurnEvent()
  private val playerFamily = allOf(CombatComponent::class, StatsComponent::class, PlayerComponent::class).get()
  private val playerEntities by lazy { engine.getEntitiesFor(playerFamily) }
  private val enemyFamily = allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get()
  private val enemyEntities by lazy { engine.getEntitiesFor(enemyFamily) }
  private val commandPools = ObjectMap<KClass<out Command>, CommandPool<out Command>>()

  init {
    //TODO find out why pooling isn't working. Only works the first time. When opening combatscreen a second time
    //it fails because the roottask of the tree is null
    // bTreeManager.library = PooledBehaviorTreeLibrary()
    gameEventDispatcher.addListener(GameEventType.DEATH, this)
  }

  override fun entityAdded(entity: Entity) {
    super.entityAdded(entity)

    //TODO how does task pooling work?

    // initialize commands
    with(entity.combatCmp) {
      learnedCommands.forEach { availableCommands[it] = obtainCommand(entity, it) }
    }

    // initialize AI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      // initialize tree
      combatAiCmp.behaviorTree = try {
        bTreeManager.createBehaviorTree(combatAiCmp.treeFilePath, entity)
      } catch (e: RuntimeException) {
        LOG.error { "Couldn't parse behavior tree '${combatAiCmp.treeFilePath}' -> fall back to default AI" }
        combatAiCmp.treeFilePath = DEFAULT_COMBAT_TREE_PATH
        bTreeManager.createBehaviorTree(DEFAULT_COMBAT_TREE_PATH, entity)
      }
      // set all possible combat targets for AI
      combatAiCmp.allTargets = entities
    }
  }

  override fun entityRemoved(entity: Entity) {
    super.entityRemoved(entity)

    // cleanup commands
    with(entity.combatCmp) {
      availableCommands.values().forEach { freeCommand(it) }
    }

    // cleanup AI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      bTreeManager.disposeBehaviorTree(combatAiCmp.treeFilePath, combatAiCmp.behaviorTree)
    }
  }

  private fun isPlayerVictorious(): Boolean {
    enemyEntities.forEach {
      if (it.isAlive) {
        return false
      }
    }
    return true
  }

  private fun isPlayerDefeated(): Boolean {
    playerEntities.forEach {
      if (it.isAlive) {
        return false
      }
    }
    return true
  }

  override fun onEvent(event: GameEvent) {
    if (event is CombatDeathEvent && !event.entity.isPlayer) {
      // remove remaining commands of dying entity
      val iter = commandQueue.iterator()
      while (iter.hasNext()) {
        val command = iter.next()
        if (command.entity == event.entity) {
          freeCommand(command)
          iter.remove()
        }
      }

      // add final commands of dying entity to queue like e.g. the death command
      event.entity.combatCmp.commandsToExecute.forEach {
        commandQueue.addFirst(it)
      }

      // update command targets by removing the dying entity
      updateCommandTargets(event.entity)
    }
  }

  override fun update(deltaTime: Float) {
    when (combatPhase) {
      CombatPhase.NEW_TURN -> {
        when {
          isPlayerDefeated() -> gameEventDispatcher.dispatchEvent(defeatEvent)
          isPlayerVictorious() -> gameEventDispatcher.dispatchEvent(victoryEvent)
          else -> updateAiCommands(deltaTime)
        }
      }
      CombatPhase.WAIT_FOR_PLAYER_ORDER -> updatePlayerCommands()
      CombatPhase.PREPARE_COMMANDS -> {
        entities.forEach {
          if (it.isAlive) {
            // only consider entities which are still alive
            prepareCommands(it)
          }
        }
        combatPhase = CombatPhase.EXECUTE_COMMANDS
        LOG.debug { "Starting new round with ${commandQueue.size} commands" }
      }
      CombatPhase.EXECUTE_COMMANDS -> {
        if (commandQueue.isEmpty) {
          // all commands executed -> start next turn
          combatPhase = CombatPhase.NEW_TURN
        } else {
          val currentCommand = commandQueue.first()
          if (currentCommand.update(deltaTime)) {
            freeCommand(currentCommand)
            commandQueue.removeFirst()
          }
        }
      }
    }
  }

  private fun updateAiCommands(deltaTime: Float) {
    if (isPlayerDefeated()) {
      // all player entities dead -> do not execute AI because it will not be possible to get a player target
      return
    }

    // sort entities in case their agility changed during combat
    forceSort()
    super.update(deltaTime)
    combatPhase = CombatPhase.WAIT_FOR_PLAYER_ORDER
    gameEventDispatcher.dispatchEvent(playerTurnEvent)
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (entity.isDead) {
      // ignore dead entities
      return
    }

    val combatCmp = entity.combatCmp

    // get new AI orders for next round; player order is added via UI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      combatAiCmp.behaviorTree.step()
      if (combatCmp.commandsToExecute.isEmpty) {
        LOG.error { "Stepping behavior tree of entity $entity did not define a combat order" }
      }
    }
  }

  private fun updatePlayerCommands() {
    playerEntities.forEach {
      if (it.combatCmp.commandsToExecute.isEmpty) {
        // not all player entities have a command
        return
      }
    }

    combatPhase = CombatPhase.PREPARE_COMMANDS
  }

  private fun prepareCommands(entity: Entity) {
    entity.combatCmp.commandsToExecute.forEach {
      commandQueue.addLast(it)
    }
  }

  private fun obtainCommand(entity: Entity, type: KClass<out Command>): Command {
    // get command type
    val commandType = if (type == Command::class) {
      LOG.error { "Trying to obtain command of type 'Command'. It must be a correct subtype" }
      CommandDefend::class
    } else {
      type
    }

    // create new command
    return commandPools.getOrPut(commandType) {
      try {
        val constructor = ClassReflection.getConstructor(commandType.java, CombatContext::class.java)
        CommandPool(constructor, combatContext)
      } catch (e: ReflectionException) {
        throw GdxRuntimeException("Could not find (CombatContext) constructor for command ${commandType.simpleName}")
      }
    }.obtain().apply { this.entity = entity }
  }

  private fun freeCommand(command: Command) {
    @Suppress("UNCHECKED_CAST")
    val pool = commandPools.get(command::class) as CommandPool<Command>
    pool.free(command)
  }

  private fun updateCommandTargets(entityToRemove: Entity) {
    commandQueue.forEach { command ->
      if (command.targets.isNotEmpty()) {
        command.targets.removeValue(entityToRemove, true)
        if (command.targets.isEmpty) {
          // no more targets left -> reassign new target
          reassignTargets(command)
        }
      }
    }
  }

  private fun reassignTargets(command: Command) {
    when (command.targetType) {
      CommandTargetType.SINGLE_TARGET -> {
        if (command.entity.isPlayer) {
          command.targets.add(randomEntity(enemyEntities))
        } else {
          command.targets.add(randomEntity(playerEntities))
        }
      }
      else -> LOG.error { "Reassign for target type ${command.targetType} is not supported" }
    }
  }

  private fun randomEntity(entities: ImmutableArray<Entity>): Entity {
    TMP_ARRAY.clear()
    entities.forEach {
      if (it.isAlive) {
        TMP_ARRAY.add(it)
      }
    }
    return TMP_ARRAY.random()
  }

  companion object {
    private const val DEFAULT_COMBAT_TREE_PATH = "ai/genericCombat.tree"
    private val LOG = logger<CombatSystem>()
    private val TMP_ARRAY = GdxArray<Entity>()
  }
}
