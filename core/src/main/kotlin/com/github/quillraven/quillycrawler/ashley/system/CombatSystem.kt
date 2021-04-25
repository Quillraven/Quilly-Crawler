package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager
import com.github.quillraven.quillycrawler.ashley.component.CombatComponent
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.combat.CombatBlackboardPool
import ktx.ashley.allOf
import ktx.log.error
import ktx.log.logger

class CombatSystem(
  private val bTreeManager: BehaviorTreeLibraryManager = BehaviorTreeLibraryManager.getInstance()
) : IteratingSystem(allOf(CombatComponent::class).get()), EntityListener {
  private val blackboardPool = CombatBlackboardPool()

  init {
    //TODO find out why pooling isn't working. Only works the first time. When opening combatscreen a second time
    //it fails because the roottask of the tree is null
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
    //TODO how does task pooling work?
    with(entity.combatCmp) {
      // initialize tree
      behaviorTree = try {
        bTreeManager.createBehaviorTree(treeFilePath, blackboardPool.obtain())
      } catch (e: RuntimeException) {
        LOG.error { "Couldn't parse behavior tree '$treeFilePath' -> fall back to default AI" }
        treeFilePath = DEFAULT_COMBAT_TREE_PATH
        bTreeManager.createBehaviorTree(DEFAULT_COMBAT_TREE_PATH, blackboardPool.obtain())
      }

      // initialize blackboard
      behaviorTree.`object`.apply {
        this.owner = entity
      }
    }
  }

  override fun entityRemoved(entity: Entity) {
    with(entity.combatCmp) {
      blackboardPool.free(behaviorTree.`object`)
      bTreeManager.disposeBehaviorTree(treeFilePath, behaviorTree)
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    entity.combatCmp.behaviorTree.step()
  }

  companion object {
    private const val DEFAULT_COMBAT_TREE_PATH = "ai/genericCombat.tree"
    private val LOG = logger<CombatSystem>()
  }
}
