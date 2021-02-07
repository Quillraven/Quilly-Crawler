package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.render
import com.github.quillraven.commons.ashley.component.transform
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.graphics.use
import ktx.log.error
import ktx.log.logger

class RenderSystem(
    private val batch: Batch,
    private val viewport: Viewport,
) : SortedIteratingSystem(
    allOf(TransformComponent::class, RenderComponent::class).get(),
    compareBy { it[TransformComponent.MAPPER] }
) {
    override fun update(deltaTime: Float) {
        forceSort()

        viewport.apply()
        batch.use(viewport.camera) {
            super.update(deltaTime)
        }
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val transform = entity.transform
        val render = entity.render

        if (render.sprite.texture == null) {
            LOG.error { "Entity '$entity' does not have a texture" }
            return
        }

        render.sprite.run {
            setScale(transform.size.x, transform.size.y)
            setPosition(
                transform.position.x - originX * (1f - scaleX),
                transform.position.y - originY * (1f - scaleY)
            )
            draw(batch)
        }
    }

    companion object {
        private val LOG = logger<RenderSystem>()
    }
}