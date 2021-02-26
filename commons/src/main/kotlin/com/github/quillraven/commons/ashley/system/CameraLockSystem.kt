package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Camera
import com.github.quillraven.commons.ashley.component.CameraLockComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.transformCmp
import ktx.ashley.allOf
import ktx.log.error
import ktx.log.logger

class CameraLockSystem(private val camera: Camera) :
    IteratingSystem(allOf(CameraLockComponent::class, TransformComponent::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (entities.size() > 1) {
            LOG.error { "There are more than 1 entities with a locked camera" }
        }

        with(entity.transformCmp.position) {
            camera.position.set(x, y, 0f)
        }
        camera.update()
    }

    companion object {
        private val LOG = logger<CameraLockSystem>()
    }
}