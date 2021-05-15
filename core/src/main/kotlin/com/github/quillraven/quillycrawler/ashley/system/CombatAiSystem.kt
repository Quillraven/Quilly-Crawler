package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.quillycrawler.ashley.component.CombatAIComponent
import com.github.quillraven.quillycrawler.ashley.component.CombatComponent
import com.github.quillraven.quillycrawler.ashley.component.StatsComponent
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.error
import ktx.log.logger

class CombatAiSystem(
  private val bTreeManager: BehaviorTreeLibraryManager = BehaviorTreeLibraryManager.getInstance()
) : IteratingSystem(allOf(CombatAIComponent::class).get()), EntityListener {
  private val combatEntities by lazy {
    engine.getEntitiesFor(
      allOf(
        CombatComponent::class,
        StatsComponent::class
      ).exclude(RemoveComponent::class).get()
    )
  }

  init {
    //TODO find out why pooling isn't working. Only works the first time. When opening combatscreen a second time
    // it fails because the roottask of the tree is null
    //TODO how does task pooling work?
    // bTreeManager.library = PooledBehaviorTreeLibrary()
  }

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    engine.addEntityListener(family, this)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
  }

  override fun entityAdded(entity: Entity) {
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
      combatAiCmp.allTargets = combatEntities
    }
  }

  override fun entityRemoved(entity: Entity) {
    // cleanup AI
    entity[CombatAIComponent.MAPPER]?.let { combatAiCmp ->
      bTreeManager.disposeBehaviorTree(combatAiCmp.treeFilePath, combatAiCmp.behaviorTree)
    }
  }

  override fun processEntity(entity: Entity?, deltaTime: Float) = Unit

  companion object {
    private const val DEFAULT_COMBAT_TREE_PATH = "ai/genericCombat.tree"
    private val LOG = logger<CombatAiSystem>()
  }
}
