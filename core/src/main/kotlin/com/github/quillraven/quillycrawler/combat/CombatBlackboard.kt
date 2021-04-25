package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.collections.GdxArray

class CombatBlackboard : Pool.Poolable {
  lateinit var owner: Entity
  val targets = GdxArray<Entity>()

  override fun reset() {
    targets.clear()
  }
}

class CombatBlackboardPool : Pool<CombatBlackboard>() {
  override fun newObject() = CombatBlackboard()
}
