package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Camera
import com.github.quillraven.commons.ashley.component.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.error
import ktx.log.logger

/**
 * System to center the [camera] on an [entity's][Entity] position. This system will only work
 * properly if there at most one entity with a [CameraLockComponent].
 *
 * The camera will be centered on the [Box2DComponent.renderPosition] if this component is available.
 * Otherwise it uses the [TransformComponent.position].
 */
class CameraLockSystem(private val camera: Camera) :
  IteratingSystem(
    allOf(CameraLockComponent::class, TransformComponent::class).exclude(RemoveComponent::class).get()
  ) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (entities.size() > 1) {
      LOG.error { "There are more than 1 entities with a locked camera" }
    }

    val box2DCmp = entity[Box2DComponent.MAPPER]
    if (box2DCmp == null) {
      with(entity.transformCmp.position) {
        camera.position.set(x, y, 0f)
      }
    } else {
      // in case an entity has a box2d component then we need to use its interpolated
      // render position to avoid sprite jitter
      camera.position.set(box2DCmp.renderPosition.x, box2DCmp.renderPosition.y, 0f)
    }
    camera.update()
  }

  companion object {
    private val LOG = logger<CameraLockSystem>()
  }
}
