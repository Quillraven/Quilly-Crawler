package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.map.MapService
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.graphics.use
import ktx.log.error
import ktx.log.logger

/**
 * System for rendering [entities][Entity] using the data of their [RenderComponent] and [TransformComponent].
 *
 * Applies the [viewport] to the [batch] before rendering each entity. The [sprite's][Sprite] position
 * is updated by either using the [Box2DComponent.renderPosition] or the [TransformComponent.position].
 *
 * The order in which entities are rendered is defined by their [TransformComponent].
 *
 * The size of the sprite is defined by [TransformComponent.size]. A size of 1 means that the [Sprite]
 * is not scaled (=100%). A size smaller 1 will shrink the sprite while a size greater 1 will increase it.
 */
class RenderSystem(
    private val batch: Batch,
    private val viewport: Viewport,
    private val camera: OrthographicCamera = viewport.camera as OrthographicCamera,
    private val mapService: MapService? = null
) : SortedIteratingSystem(
    allOf(TransformComponent::class, RenderComponent::class).get(),
    compareBy { it[TransformComponent.MAPPER] }
) {
    /**
     * Sorts the entities, applies the viewport to the batch and renders each entity.
     */
    override fun update(deltaTime: Float) {
        forceSort()

        viewport.apply()
        // TODO introduce empty DefaultMapService to get rid of multiple null checks (?.)
        mapService?.setViewBounds(camera)
        batch.use(camera) {
            mapService?.renderBackground()
            super.update(deltaTime)
            mapService?.renderForeground()
        }
    }

    /**
     * Renders an [entity] by using its [sprite][RenderComponent.sprite].
     */
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val transformCmp = entity.transformCmp
        val renderCmp = entity.renderCmp
        val box2dCmp = entity[Box2DComponent.MAPPER]

        if (renderCmp.sprite.texture == null) {
            LOG.error { "Entity '$entity' does not have a texture" }
            return
        }

        renderCmp.sprite.run {
            // scale sprite by the entity's size
            setScale(transformCmp.size.x, transformCmp.size.y)

            // update sprite position according to the physic's interpolated position
            // or normal transform position
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

            // render entity
            draw(batch)
        }
    }

    companion object {
        private val LOG = logger<RenderSystem>()
    }
}