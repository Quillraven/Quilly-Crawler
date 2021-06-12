package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeCmp
import ktx.ashley.allOf

/**
 * System to remove [entities][Entity] from the engine at the beginning/end of a frame using the
 * data of their [RemoveComponent].
 */
class RemoveSystem : IteratingSystem(allOf(RemoveComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val removeCmp = entity.removeCmp

    removeCmp.delay -= deltaTime
    if (removeCmp.delay <= 0f) {
      engine.removeEntity(entity)
    }
  }
}
