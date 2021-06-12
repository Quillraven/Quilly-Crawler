package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to modify the color of an [Entity] which has a [RenderComponent].
 *
 * Use [fromColor] to define the starting color of the fade effect. Usually, that should be the sprite's color.
 * Use [toColor] to define the final color of the fade effect.
 * Use [duration] to define how long the effect should need in seconds.
 */
class FadeComponent : Component, Pool.Poolable {
  val fromColor = Color(1f, 1f, 1f, 1f)
  val toColor = Color(1f, 1f, 1f, 1f)
  var duration = 0f
    set(value) {
      currentDuration = 0f
      field = value
    }
  internal var currentDuration = 0f

  override fun reset() {
    fromColor.set(1f, 1f, 1f, 1f)
    toColor.set(1f, 1f, 1f, 1f)
    duration = 0f
    currentDuration = 0f
  }

  companion object {
    val MAPPER = mapperFor<FadeComponent>()
  }
}

/**
 * Returns a [FadeComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.fadeCmp: FadeComponent
  get() = this[FadeComponent.MAPPER]
    ?: throw GdxRuntimeException("FadeComponent for entity '$this' is null")

/**
 * Adds a [FadeComponent] to the entity with the given values. If the entity does not have a
 * [RenderComponent] then this function is doing nothing.
 */
fun Entity.fadeTo(engine: Engine, r: Float, g: Float, b: Float, a: Float, duration: Float) {
  val entity = this
  val renderCmp = entity[RenderComponent.MAPPER] ?: return

  with(entity[FadeComponent.MAPPER] ?: entity.addAndReturn(engine.createComponent(FadeComponent::class.java))) {
    fromColor.set(renderCmp.sprite.color)
    toColor.set(r, g, b, a)
    this.duration = duration
  }
}
