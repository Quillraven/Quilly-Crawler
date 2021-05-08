package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.commons.ashley.component.FadeComponent
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.fadeCmp
import com.github.quillraven.commons.ashley.component.renderCmp
import ktx.ashley.allOf

/**
 * System for modifying an [entity's][Entity] color by linearly interpolate its [Sprite] color
 * from the [FadeComponent] [FadeComponent.fromColor] to the [FadeComponent.toColor] over the
 * period of [FadeComponent.duration] seconds.
 */
class FadeSystem : IteratingSystem(allOf(RenderComponent::class, FadeComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val fadeCmp = entity.fadeCmp

    fadeCmp.currentDuration += deltaTime
    val progress = if (fadeCmp.duration == 0f || fadeCmp.currentDuration >= fadeCmp.duration) {
      1f
    } else {
      fadeCmp.currentDuration / fadeCmp.duration
    }

    with(entity.renderCmp.sprite) {
      setColor(
        MathUtils.lerp(fadeCmp.fromColor.r, fadeCmp.toColor.r, progress),
        MathUtils.lerp(fadeCmp.fromColor.g, fadeCmp.toColor.g, progress),
        MathUtils.lerp(fadeCmp.fromColor.b, fadeCmp.toColor.b, progress),
        MathUtils.lerp(fadeCmp.fromColor.a, fadeCmp.toColor.a, progress)
      )
    }

    if (progress == 1f) {
      entity.remove(FadeComponent::class.java)
    }
  }
}
