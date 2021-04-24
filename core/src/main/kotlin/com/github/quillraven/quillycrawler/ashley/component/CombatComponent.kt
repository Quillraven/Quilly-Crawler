package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool

/**
 * Marks an entity as part of a combat. It will be part of the CombatSystem iteration.
 */
class CombatComponent : Component, Pool.Poolable {

  override fun reset() {
  }
}
