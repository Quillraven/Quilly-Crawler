package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to modify the size of an [Entity] which has a [TransformComponent].
 *
 * Use [fromSize] to define the starting size of the resize effect. Usually, that should be the transform's size.
 * Use [toSize] to define the final size of the resize effect.
 * Use [duration] to define how long the effect should need in seconds.
 */
class ResizeComponent : Component, Pool.Poolable {
  val fromSize = Vector2()
  val toSize = Vector2()
  var duration = 0f
    set(value) {
      currentDuration = 0f
      field = value
    }
  internal var currentDuration = 0f

  override fun reset() {
    fromSize.set(0f, 0f)
    toSize.set(0f, 0f)
    duration = 0f
    currentDuration = 0f
  }

  companion object {
    val MAPPER = mapperFor<ResizeComponent>()
  }
}

/**
 * Returns a [ResizeComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.resizeCmp: ResizeComponent
  get() = this[ResizeComponent.MAPPER]
    ?: throw GdxRuntimeException("ResizeComponent for entity '$this' is null")

/**
 * Adds a [ResizeComponent] to the entity with the given values. If the entity does not have a
 * [TransformComponent] then this function is doing nothing.
 */
fun Entity.resizeTo(engine: Engine, newX: Float, newY: Float, duration: Float) {
  val entity = this
  val transformCmp = entity[TransformComponent.MAPPER] ?: return

  with(entity[ResizeComponent.MAPPER] ?: entity.addAndReturn(engine.createComponent(ResizeComponent::class.java))) {
    fromSize.set(transformCmp.size)
    toSize.set(newX, newY)
    this.duration = duration
  }
}
