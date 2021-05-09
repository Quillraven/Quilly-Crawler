package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.Command
import com.github.quillraven.quillycrawler.combat.effect.CommandEffectDefend
import com.github.quillraven.quillycrawler.combat.effect.CommandEffectUndefined
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

private enum class CombatPhase {
  UNDEFINED, UPDATE_AI_ORDERS, WAIT_FOR_PLAYER_ORDER, EXECUTE_ORDERS
}

class CombatSystem(
  audioService: AudioService,
  private val gameEventDispatcher: GameEventDispatcher,
  private val bTreeManager: BehaviorTreeLibraryManager = BehaviorTreeLibraryManager.getInstance()
) : GameEventListener, SortedIteratingSystem(
  allOf(CombatComponent::class, StatsComponent::class).exclude(RemoveComponent::class).get(),
  // Agility defines the order how entities are executed.
  // Higher agility means an entity executes its combat order faster.
  compareBy { -it.statsCmp[StatsType.AGILITY] }
) {
  private var combatPhase = CombatPhase.UNDEFINED
  private var executeOrders = false
  private var allOrdersExecuted = false
  private val currentOrder by lazy { Command(engine, audioService) }
  private val victoryEvent = CombatVictoryEvent()
  private val defeatEvent = CombatDefeatEvent()
  private val playerTurnEvent = CombatPlayerTurnEvent()

  init {
    //TODO find out why pooling isn't working. Only works the first time. When opening combatscreen a second time
    //it fails because the roottask of the tree is null
    // bTreeManager.library = PooledBehaviorTreeLibrary()
    gameEventDispatcher.addListener(GameEventType.DEATH, this)
  }

  override fun entityAdded(entity: Entity) {
    super.entityAdded(entity)

    //TODO how does task pooling work?

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
    // cleanup AI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      bTreeManager.disposeBehaviorTree(combatAiCmp.treeFilePath, combatAiCmp.behaviorTree)
    }
  }

  private fun checkAllOrders(): Boolean {
    // check if every entity defined its next combat order
    var allEntitiesHaveOrder = true

    for (entity in entities) {
      if (entity.combatCmp.effect == CommandEffectUndefined) {
        allEntitiesHaveOrder = false
        break
      } else if (allOrdersExecuted) {
        // all orders were executed the last time this system was running but now at least one
        // entity still has an order outstanding.
        // This can happen e.g. if an entity dies within the DamageEmitterSystem and changes its order
        // to the death order.
        // Another example would be if a boss transforms to its next phase after defeating one of its phases.
        allOrdersExecuted = false
      }
    }

    return allEntitiesHaveOrder
  }

  private fun executeOrder(deltaTime: Float) {
    allOrdersExecuted = false

    for (i in 0 until entities.size()) {
      val entity = entities[i]
      val combatCmp = entity.combatCmp
      if (combatCmp.effect == CommandEffectUndefined) {
        // entity already executed order
        continue
      }

      if (currentOrder.effect == CommandEffectUndefined) {
        // initialize current order
        currentOrder.reset()
        currentOrder.source = entity
        currentOrder.effect = combatCmp.effect
        currentOrder.targets.addAll(combatCmp.orderTargets)
      } else if (currentOrder.source != entity) {
        // entity does not match current order source -> ignore it
        // this can happen e.g. if
        // entity 0 finished its order, entity 1 is currently in progress but entity 0 gets another order in between
        continue
      }

      if (currentOrder.update(deltaTime)) {
        // order finished -> remove effect to prepare entity for next round
        LOG.debug { "ORDER FINISHED for $entity" }
        currentOrder.effect = CommandEffectUndefined
        combatCmp.effect = CommandEffectUndefined
        combatCmp.orderTargets.clear()
        allOrdersExecuted = i == entities.size() - 1
      }

      // current order not finished yet -> wait for it to be finished before going to next order
      // OR current order was finished -> execute remaining systems in engine before going to next order
      break
    }
  }

  override fun onEvent(event: GameEvent) {
    if (event is CombatDeathEvent) {
      // check defeat or victory
      if (isPlayerVictorious()) {
        combatPhase = CombatPhase.UNDEFINED
        gameEventDispatcher.dispatchEvent(victoryEvent)
      } else if (isPlayerDefeated()) {
        combatPhase = CombatPhase.UNDEFINED
        gameEventDispatcher.dispatchEvent(defeatEvent)
      }
    }
  }

  override fun update(deltaTime: Float) {
    // update phase of combat
    // either retrieve orders for next round
    // or, if all entities have an order, then execute them one by one
    executeOrders = checkAllOrders() || (executeOrders && !allOrdersExecuted)

    if (executeOrders) {
      // every entity has an order -> execute one by one
      combatPhase = CombatPhase.EXECUTE_ORDERS
      executeOrder(deltaTime)
    } else if (combatPhase == CombatPhase.EXECUTE_ORDERS || combatPhase == CombatPhase.UNDEFINED) {
      combatPhase = CombatPhase.UPDATE_AI_ORDERS
      // sort entities in case their agility changed during combat
      forceSort()
      // update AI orders
      super.update(deltaTime)
      combatPhase = CombatPhase.WAIT_FOR_PLAYER_ORDER
      gameEventDispatcher.dispatchEvent(playerTurnEvent)
    }
  }

  private fun isPlayerVictorious(): Boolean {
    var allEnemiesDead = true

    entities.forEach {
      if (it[PlayerComponent.MAPPER] == null && it.statsCmp[StatsType.LIFE] > 0f) {
        allEnemiesDead = false
      }
    }

    return allEnemiesDead
  }

  private fun isPlayerDefeated(): Boolean {
    var allPlayersDead = true

    entities.forEach {
      if (it[PlayerComponent.MAPPER] != null && it.statsCmp[StatsType.LIFE] > 0f) {
        allPlayersDead = false
      }
    }

    return allPlayersDead
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val combatCmp = entity.combatCmp
    if (combatCmp.effect != CommandEffectUndefined) {
      // effect already chosen -> do nothing
      return
    }

    // get new AI orders for next round; player order is added via UI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      combatAiCmp.behaviorTree.step()
      if (combatCmp.effect == CommandEffectUndefined) {
        LOG.error { "Stepping behavior tree of entity $entity did not define a combat order" }
        combatCmp.effect = CommandEffectDefend
      }
    }
  }

  companion object {
    private const val DEFAULT_COMBAT_TREE_PATH = "ai/genericCombat.tree"
    private val LOG = logger<CombatSystem>()
  }
}
