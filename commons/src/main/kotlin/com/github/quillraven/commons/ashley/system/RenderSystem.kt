package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.map.DefaultMapService
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.commons.map.TiledMapService
import com.github.quillraven.commons.shader.EmptyShaderService
import com.github.quillraven.commons.shader.ShaderService
import ktx.ashley.allOf
import ktx.ashley.exclude
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
 *
 * Use [mapService] to define a specific [MapService] that should be used for 2d-map rendering like [TiledMapService].
 * [MapService.renderBackground] is called before any entity is rendered.
 * [MapService.renderForeground] is called afterwards.
 *
 * Use [shaderService] in case you want to have custom render behavior using [ShaderProgram].
 * [ShaderService.postRenderEntities] is called after entities are rendered and before [MapService.renderForeground].
 */
class RenderSystem(
  private val batch: Batch,
  private val viewport: Viewport,
  private val camera: OrthographicCamera = viewport.camera as OrthographicCamera,
  private val mapService: MapService = DefaultMapService,
  private val shaderService: ShaderService = EmptyShaderService(batch)
) : SortedIteratingSystem(
  allOf(TransformComponent::class, RenderComponent::class).exclude(RemoveComponent::class).get(),
  compareBy { it[TransformComponent.MAPPER] }
) {
  /**
   * Sorts the entities, applies the viewport to the batch and renders each entity.
   *
   * If a [mapService] is defined then its renderBackground and renderForeground functions
   * are called accordingly.
   *
   * Calls [ShaderService.postRenderEntities] after entities are rendered using the [ShaderService.activeShader].
   */
  override fun update(deltaTime: Float) {
    // always sort entities in case their y-/z-coordinate was modified
    forceSort()

    // apply render boundaries
    viewport.apply()
    mapService.setViewBounds(camera)

    // apply active shader - per default it is the normal shader from the batch
    if (shaderService.activeShader != batch.shader) {
      batch.shader = shaderService.activeShader
    }

    // render map background and entities
    shaderService.preRender(viewport)
    batch.use(camera) {
      mapService.renderBackground()
      super.update(deltaTime)
      shaderService.postRenderEntities(entities)
      mapService.renderForeground()
    }
    shaderService.postRender(viewport)
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
      draw(batch, batch.color.a)
    }
  }

  companion object {
    private val LOG = logger<RenderSystem>()
  }
}
