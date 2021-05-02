package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.btree.BehaviorTree
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class CombatAIComponent : Component, Pool.Poolable {
  var treeFilePath = ""
  lateinit var behaviorTree: BehaviorTree<Entity>
  lateinit var allTargets: ImmutableArray<Entity>

  override fun reset() {
    treeFilePath = ""
  }

  companion object {
    val MAPPER = mapperFor<CombatAIComponent>()
  }
}

val Entity.combatAICmp: CombatAIComponent
  get() = this[CombatAIComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatAIComponent for entity '$this' is null")
