package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.ShakeComponent
import com.github.quillraven.commons.ashley.component.renderCmp
import com.github.quillraven.commons.ashley.component.shakeCmp
import ktx.ashley.allOf

/**
 * System for shaking an [entity's][Entity] [Sprite].
 * An [Entity] must have a [ShakeComponent] and [RenderComponent].
 * The [RenderComponent.offset] is adjusted according to the shake information of the [ShakeComponent].
 * The maximum offset is defined by [ShakeComponent.maxDistortion].
 *
 * At the end of a shake the original [RenderComponent.offset] is restored.
 */
class ShakeSystem : IteratingSystem(allOf(ShakeComponent::class, RenderComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    with(entity.shakeCmp) {
      val renderCmp = entity.renderCmp

      if (currentDuration == 0f) {
        // start of shake -> store original offset to restore it later on
        origOffset.set(renderCmp.offset)
      }

      if (currentDuration < duration) {
        // shake not finished -> update offset
        val currentPower = maxDistortion * ((duration - currentDuration) / duration)

        renderCmp.offset.x = origOffset.x + MathUtils.random(-1f, 1f) * currentPower
        renderCmp.offset.y = origOffset.y + MathUtils.random(-1f, 1f) * currentPower

        currentDuration += deltaTime
      } else {
        // shake finished -> restore original offset
        renderCmp.offset.set(origOffset)
        entity.remove(ShakeComponent::class.java)
      }
    }
  }
}
