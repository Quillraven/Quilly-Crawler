package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.CombatOrder
import com.github.quillraven.quillycrawler.combat.CombatOrderEffectUndefined
import com.github.quillraven.quillycrawler.event.CombatVictoryEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class CombatSystem(
  audioService: AudioService,
  private val gameEventDispatcher: GameEventDispatcher,
  private val bTreeManager: BehaviorTreeLibraryManager = BehaviorTreeLibraryManager.getInstance()
) : SortedIteratingSystem(
  allOf(CombatComponent::class, StatsComponent::class).exclude(RemoveComponent::class).get(),
  // Agility defines the order how entities are executed.
  // Higher agility means an entity executes its combat order faster.
  compareBy { -it.statsCmp[StatsType.AGILITY] }
) {
  private var executeOrders = false
  private var allOrdersExecuted = false
  private val currentOrder by lazy { CombatOrder(engine, audioService) }

  init {
    //TODO find out why pooling isn't working. Only works the first time. When opening combatscreen a second time
    //it fails because the roottask of the tree is null
    // bTreeManager.library = PooledBehaviorTreeLibrary()
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

  override fun update(deltaTime: Float) {
    if (isPlayerVictorious()) {
      gameEventDispatcher.dispatchEvent(CombatVictoryEvent())
      return
    }

    // check if every entity defined its next combat order
    var allEntitiesHaveEffect = true
    for (entity in entities) {
      if (entity.combatCmp.effect == CombatOrderEffectUndefined) {
        allEntitiesHaveEffect = false
        break
      } else if (allOrdersExecuted) {
        allOrdersExecuted = false
      }
    }
    executeOrders = allEntitiesHaveEffect || (executeOrders && !allOrdersExecuted)

    //TODO implement combat order logic with update call; as long as update doesn't return true the order is not done
    // make a generic CombatOrder class that provides generic functions for dealing damage, healing, playing a sound, etc.
    // an order is linked to an effect like attack or firebolt and calls its start,update and end function
    // effects are objects (=singletons)
    // damage, healing, etc. is handled via components and their system
    // all these events need to be dispatched as well so that the UI can react on it.
    // in addition we need buffs (=entities) that are permanent or temporary effects only during combat like
    // increasing healing effects by 200% or reducing physical damage to 0 for the next 3 attacks. they are also part
    // of the event pipeline and modify the event data like dealt damage or healing amount, etc.
    // in case of a temporary buff the buff entity removes itself from the engine e.g. after 3 event notifications

    if (executeOrders) {
      allOrdersExecuted = false

      // every entity has an order -> execute one by one
      for (i in 0 until entities.size()) {
        val entity = entities[i]
        val combatCmp = entity.combatCmp
        if (combatCmp.effect == CombatOrderEffectUndefined) {
          // entity already executed order
          continue
        }

        if (currentOrder.effect == CombatOrderEffectUndefined) {
          // initialize current order
          currentOrder.reset()
          currentOrder.source = entity
          currentOrder.effect = combatCmp.effect
          currentOrder.targets.addAll(combatCmp.orderTargets)
        } else if (currentOrder.source != entity) {
          // entity does not match current order source -> ignore it
          LOG.error { "This should never happen. Quilly, you failed miserably!" }
          continue
        }

        if (currentOrder.update(deltaTime)) {
          // order finished -> remove effect to prepare entity for next round
          LOG.debug { "ORDER FINISHED for $entity" }
          currentOrder.effect = CombatOrderEffectUndefined
          combatCmp.effect = CombatOrderEffectUndefined
          combatCmp.orderTargets.clear()
          allOrdersExecuted = i == entities.size() - 1
        }

        // current order not finished yet -> wait for it to be finished before going to next order
        // OR current order was finished -> execute remaining systems in engine before going to next order
        break
      }
    } else {
      // sort entities in case their agility changed during combat
      forceSort()
      // update AI orders
      super.update(deltaTime)
    }
  }

  private fun isPlayerVictorious(): Boolean {
    var allEnemiesDead = true

    entities.forEach {
      if (it[PlayerComponent.MAPPER] == null) {
        allEnemiesDead = false
      }
    }

    return allEnemiesDead
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val combatCmp = entity.combatCmp
    if (combatCmp.effect != CombatOrderEffectUndefined) {
      // effect already chosen -> do nothing
      return
    }

    // get new AI orders for next round; player order is added via UI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      combatAiCmp.behaviorTree.step()
      if (combatCmp.effect == CombatOrderEffectUndefined) {
        LOG.error { "Stepping behavior tree of entity $entity did not define a combat order" }
      }
    }
  }

  companion object {
    private const val DEFAULT_COMBAT_TREE_PATH = "ai/genericCombat.tree"
    private val LOG = logger<CombatSystem>()
  }
}
