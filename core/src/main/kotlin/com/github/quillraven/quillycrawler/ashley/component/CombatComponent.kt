package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.BehaviorTree
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.CombatBlackboard
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Marks an entity as part of a combat. It will be part of the CombatSystem iteration.
 */
class CombatComponent : Component, Pool.Poolable {
  var treeFilePath = ""
  lateinit var behaviorTree: BehaviorTree<CombatBlackboard>

  override fun reset() {
    treeFilePath = ""
  }

  companion object {
    val MAPPER = mapperFor<CombatComponent>()
  }
}

val Entity.combatCmp: CombatComponent
  get() = this[CombatComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatComponent for entity '$this' is null")
