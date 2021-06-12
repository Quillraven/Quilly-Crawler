package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.commons.ashley.component.ResizeComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.resizeCmp
import com.github.quillraven.commons.ashley.component.transformCmp
import ktx.ashley.allOf

/**
 * System for modifying an [entity's][Entity] size by linearly interpolate its [TransformComponent.size]
 * from the [ResizeComponent] [ResizeComponent.fromSize] to the [ResizeComponent.toSize] over the
 * period of [ResizeComponent.duration] seconds.
 */
class ResizeSystem : IteratingSystem(allOf(TransformComponent::class, ResizeComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val resizeCmp = entity.resizeCmp

    resizeCmp.currentDuration += deltaTime
    val progress = if (resizeCmp.duration == 0f || resizeCmp.currentDuration >= resizeCmp.duration) {
      1f
    } else {
      resizeCmp.currentDuration / resizeCmp.duration
    }

    with(entity.transformCmp) {
      size.set(
        MathUtils.lerp(resizeCmp.fromSize.x, resizeCmp.toSize.x, progress),
        MathUtils.lerp(resizeCmp.fromSize.y, resizeCmp.toSize.y, progress)
      )
    }

    if (progress == 1f) {
      entity.remove(ResizeComponent::class.java)
    }
  }
}
