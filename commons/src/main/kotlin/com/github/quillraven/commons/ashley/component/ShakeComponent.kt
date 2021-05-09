package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.ShakeSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to shake an entity's [Sprite] via the [ShakeSystem].
 *
 * Use [maxDistortion] to define the maximum offset in world units of the shake.
 *
 * Use [duration] to set how long the shake should last.
 */
class ShakeComponent : Component, Pool.Poolable {
  var maxDistortion = 0f // in world units
  var duration = 0f
  internal var origOffset = Vector2()
  internal var currentDuration = 0f

  override fun reset() {
    maxDistortion = 0f
    duration = 0f
    currentDuration = 0f
    origOffset.set(0f, 0f)
  }

  companion object {
    val MAPPER = mapperFor<ShakeComponent>()
  }
}

/**
 * Returns a [ShakeComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.shakeCmp: ShakeComponent
  get() = this[ShakeComponent.MAPPER]
    ?: throw GdxRuntimeException("ShakeComponent for entity '$this' is null")

/**
 * Adds or updates the [ShakeComponent] of an [Entity] with the given [maxDistortion] and [duration].
 */
fun Entity.shake(engine: Engine, maxDistortion: Float, duration: Float) {
  with(this[ShakeComponent.MAPPER] ?: this.addAndReturn(engine.createComponent(ShakeComponent::class.java))) {
    this.maxDistortion = maxDistortion
    this.duration = currentDuration + duration
  }
}
