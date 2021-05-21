package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.OrderedSet
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.combat.command.CommandDeath
import com.github.quillraven.quillycrawler.combat.command.CommandPools
import com.github.quillraven.quillycrawler.combat.command.CommandTargetType
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.collections.*
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class CombatSystem(
  combatContext: CombatContext,
  private val gameEventDispatcher: GameEventDispatcher,
) : GameEventListener, SortedIteratingSystem(
  allOf(CombatComponent::class, StatsComponent::class).exclude(RemoveComponent::class).get(),
  // Agility defines the order how entities are executed.
  // Higher agility means an entity executes its command faster.
  compareBy { it.statsCmp[StatsType.AGILITY] }
) {
  private val commandPools = CommandPools(combatContext)
  private val playerFamily = allOf(CombatComponent::class, StatsComponent::class, PlayerComponent::class).get()
  private val playerEntities by lazy { engine.getEntitiesFor(playerFamily) }
  private val enemyFamily = allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get()
  private val enemyEntities by lazy { engine.getEntitiesFor(enemyFamily) }
  private var newTurn = true
  private var playerCommand: Command? = null
  private val commands = OrderedSet<Command>()
  private var currentCommand: Command? = null

  init {
    gameEventDispatcher.addListener(GameEventType.COMBAT_COMMAND_ADDED, this)
    gameEventDispatcher.addListener(GameEventType.COMBAT_COMMAND_PLAYER, this)
    gameEventDispatcher.addListener(GameEventType.DEATH, this)
    gameEventDispatcher.addListener(GameEventType.COMBAT_CLEAR_COMMANDS, this)
  }

  override fun entityAdded(entity: Entity) {
    super.entityAdded(entity)
    entity.combatCmp.eventDispatcher = gameEventDispatcher
  }

  override fun entityRemoved(entity: Entity) {
    super.entityRemoved(entity)

    // cleanup commands
    with(entity.combatCmp) {
      availableCommands.values().forEach { commandPools.freeCommand(it) }
    }
  }

  override fun onEvent(event: GameEvent) {
    when (event) {
      is CombatCommandAddedEvent -> {
        // new command added -> add it to commands to execute
        // Note: commands are added and remove similar to a stack with last in first out behavior
        // This means that e.g. if a DeathCommand occurs after an AttackCommand is finished then the
        // DeathCommand will be executed before any other of the remaining commands are executed
        if (event.command in commands) {
          // command already part of current list -> remove it first before adding it again
          // this can happen e.g. if an entity has a command added to the commands list and as a response
          // to another command that is getting executed it will execute the command again
          commands.remove(event.command)
        }
        commands.add(event.command)
        debugTurnCommand()
      }
      is CombatCommandPlayerEvent -> {
        // player gave command for next turn -> this will start a combat turn in 'update'
        playerCommand = event.command
      }
      is CombatDeathEvent -> {
        // entity died -> check victory / defeat conditions
        val allPlayersDead = allEntitiesDead(playerEntities)
        if (allPlayersDead || allEntitiesDead(enemyEntities)) {
          // combat is over and either a victory or defeat happened -> wait for next combat
          // Note: this is triggered out of [update] meaning that [cleanupTurn] is called after this
          if (allPlayersDead) {
            LOG.debug { "PLAYER defeat" }
            gameEventDispatcher.dispatchEvent<CombatDefeatEvent>()
          } else {
            LOG.debug { "PLAYER victory" }
            gameEventDispatcher.dispatchEvent<CombatVictoryEvent>()
          }

          // clear commands to execute to run [cleanupTurn] in [update]
          commands.clear()
        } else {
          // remove remaining commands of dying entity and redirect commands that target this entity
          val iterator = commands.iterator()
          while (iterator.hasNext()) {
            val cmd = iterator.next()
            if (cmd == currentCommand) {
              continue
            }

            if (cmd.entity == event.entity) {
              // remove command of dying entity
              iterator.remove()
            } else if (event.entity in cmd.targets) {
              // reassign targets if necessary
              reassignTargets(cmd, event.entity)
            }
          }
          debugTurnCommand()
        }
      }
      is CombatClearCommandsEvent -> {
        // clear any commands of the entity
        val iterator = commands.iterator()
        while (iterator.hasNext()) {
          val cmd = iterator.next()
          if (cmd == currentCommand || cmd.entity != event.entity) {
            // command not related to event entity
            continue
          }

          iterator.remove()
        }

      }
      else -> {
        LOG.error { "Received unsupported event" }
      }
    }
  }

  /**
   * This method checks the [CombatComponent.defeated] flag of each entity of [entities].
   * The flag is set in [CommandDeath] when an entity is dead and executed its death animation.
   *
   * Returns true if and only if all entities have this flag set to true.
   */
  private fun allEntitiesDead(entities: ImmutableArray<Entity>): Boolean {
    entities.forEach {
      if (!it.combatCmp.defeated) {
        return false
      }
    }
    return true
  }

  override fun update(deltaTime: Float) {
    // learn commands if needed. refer to [processEntity]
    super.update(deltaTime)

    if (newTurn) {
      // notify UI that player can give input for next turn
      newTurn = false
      gameEventDispatcher.dispatchEvent<CombatPlayerTurnEvent>()
      return
    }

    // wait until player made decision for his next move
    val playerCmd = playerCommand ?: return

    if (currentCommand == null) {
      // start of new turn -> sort entities by agility
      forceSort()
      super.update(0f)

      // add commands to execute for this turn. refer to [onEvent]
      commands.clear()
      entities.forEach { entity ->
        if (entity.isAlive) {
          val combatAiCmp = entity[CombatAIComponent.MAPPER]
          if (combatAiCmp != null) {
            // for AI entities simply step the tree which will add the necessary commands
            combatAiCmp.behaviorTree.step()
          } else if (entity.isPlayerCombatEntity) {
            // for a player controlled entity use the [playerCommand] that is given via the UI
            entity.combatCmp.newCommand(playerCmd)
          }
        }
      }

      // set current command to execute
      currentCommand = if (commands.isEmpty) null else commands.orderedItems()[commands.size - 1]
      return
    }

    // player command and all other entity commands are given -> execute them one by one
    val cmdToExecute = currentCommand
    if (cmdToExecute != null && cmdToExecute.update(deltaTime)) {
      // command finished -> go to next command
      cmdToExecute.reset()
      commands.remove(cmdToExecute)

      if (cmdToExecute is CommandDeath) {
        // dispatch death event in case of death commands
        gameEventDispatcher.dispatchEvent<CombatDeathEvent> { this.entity = cmdToExecute.entity }
      }

      currentCommand = if (commands.isEmpty) null else commands.orderedItems()[commands.size - 1]
      if (currentCommand == null) {
        // if there is no command left go to next turn
        cleanupTurn()
      }
    }
  }

  /**
   * Debugs information of [commands] by printing each command, its entity and its entity's agility
   */
  private fun debugTurnCommand() {
    if (Gdx.app.logLevel != Application.LOG_DEBUG) {
      return
    }

    LOG.debug { "Turn Commands:" }
    commands.forEach { cmd ->
      val entity = cmd.entity
      val cmdName = cmd::class.simpleName
      LOG.debug { "$cmdName: ${entity.statsCmp[StatsType.AGILITY]} ${if (entity.isPlayer) "PLAYER" else "ENEMY"} $entity" }
    }
  }

  /**
   * Resets commands and flags to start a new turn
   */
  private fun cleanupTurn() {
    LOG.debug { "End of turn" }
    playerCommand = null
    currentCommand = null
    commands.clear()
    newTurn = true
  }

  /**
   * Updates [CombatComponent.availableCommands] for the given [entity] by creating a [Command] instances
   * for each command in [CombatComponent.commandsToLearn].
   */
  override fun processEntity(entity: Entity, deltaTime: Float) {
    with(entity.combatCmp) {
      // add commands that should be learned to the entity
      if (commandsToLearn.isNotEmpty()) {
        commandsToLearn.iterate { commandType, iterator ->
          availableCommands[commandType] = commandPools.obtainCommand(entity, commandType)
          iterator.remove()
        }
      }
    }
  }

  /**
   * This function gets called when the dying entity [removedEntity] is part of the given [command]'s targets.
   * If the command is of [CommandTargetType.SINGLE_TARGET] then a new random entity is added as a target instead.
   *
   * If [removedEntity] is an enemy then a random entity from [enemyEntities] is used instead.
   * Otherwise a random entity from [playerEntities] is used.
   */
  private fun reassignTargets(command: Command, removedEntity: Entity) {
    // remove entity from targets
    command.targets.removeValue(removedEntity, true)

    // get new target
    when (command.targetType) {
      CommandTargetType.SINGLE_TARGET -> {
        if (enemyEntities.contains(removedEntity, true)) {
          addRandomEntity(command.targets, enemyEntities)
        } else {
          addRandomEntity(command.targets, playerEntities)
        }
      }
      else -> LOG.error { "Reassign for target type ${command.targetType} is not supported" }
    }
  }

  /**
   * Adds a random [Entity] from [entities] which is still alive to the [targets] array.
   * If there is no entity alive then targets is not updated.
   */
  private fun addRandomEntity(targets: GdxArray<Entity>, entities: ImmutableArray<Entity>) {
    // get alive targets
    TMP_ARRAY.clear()
    entities.forEach {
      if (it.isAlive) {
        TMP_ARRAY.add(it)
      }
    }

    if (TMP_ARRAY.isNotEmpty()) {
      // there are alive entities -> pick a random one
      targets.add(TMP_ARRAY.random())
    }
  }

  companion object {
    private val LOG = logger<CombatSystem>()
    private val TMP_ARRAY = GdxArray<Entity>()
  }
}
