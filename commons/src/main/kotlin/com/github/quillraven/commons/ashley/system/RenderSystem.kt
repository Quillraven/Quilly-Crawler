package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.*
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
        val transformCmp = entity.transformCmp
        val renderCmp = entity.renderCmp
        val box2dCmp = entity[Box2DComponent.MAPPER]

        if (renderCmp.sprite.texture == null) {
            LOG.error { "Entity '$entity' does not have a texture" }
            return
        }

        renderCmp.sprite.run {
            setScale(transformCmp.size.x, transformCmp.size.y)

            if (box2dCmp == null) {
                setPosition(
                    transformCmp.position.x - originX * (1f - scaleX),
                    transformCmp.position.y - originY * (1f - scaleY)
                )
            } else {
                setPosition(
                    box2dCmp.renderPosition.x - originX * (1f - scaleX),
                    box2dCmp.renderPosition.y - originY * (1f - scaleY)
                )
            }

            draw(batch)
        }
    }

    companion object {
        private val LOG = logger<RenderSystem>()
    }
}