package com.github.quillraven.quillycrawler.ashley.shader

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.github.quillraven.commons.ashley.component.Box2DComponent
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.commons.shader.PostProcessRenderer
import com.github.quillraven.commons.shader.Shader
import com.github.quillraven.commons.shader.ShaderService
import com.github.quillraven.quillycrawler.ashley.component.InteractComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerComponent
import com.github.quillraven.quillycrawler.ashley.component.interactCmp
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.graphics.use

class DefaultPostProcessRenderer(
  private val shaderService: ShaderService,
  engine: Engine
) : PostProcessRenderer {
  private val actionablePlayerEntities =
    engine.getEntitiesFor(allOf(PlayerComponent::class, InteractComponent::class).get())

  override fun postProcess(batch: Batch, entities: ImmutableArray<Entity>, mapService: MapService) {
    actionablePlayerEntities.forEach { playerEntity ->
      playerEntity.interactCmp.entitiesInRange.forEach { inRangeEntity ->
        val renderCmp = inRangeEntity[RenderComponent.MAPPER]
        val transformCmp = inRangeEntity[TransformComponent.MAPPER]

        if (renderCmp != null && transformCmp != null) {
          // TODO replace it with specific outline shader getter where you must specify the color
          val shaderProgram = shaderService.shader(Shader.OUTLINE_SHADER).apply {
            setUniformf(getUniformLocation("u_outlineColor"), Color.RED)
          }
          batch.shader = shaderProgram
          batch.use {
            renderEntityOutline(transformCmp, renderCmp, inRangeEntity[Box2DComponent.MAPPER], batch, shaderProgram)
          }
          batch.shader = null
        }
      }
    }
  }

  fun renderEntityOutline(
    transformCmp: TransformComponent,
    renderCmp: RenderComponent,
    box2dCmp: Box2DComponent?,
    batch: Batch,
    shaderProgram: ShaderProgram
  ) {
    if (renderCmp.sprite.texture == null) {
      return
    }

    shaderProgram.setUniformf(
      shaderProgram.getUniformLocation("u_textureSize"),
      renderCmp.sprite.texture.width.toFloat(),
      renderCmp.sprite.texture.height.toFloat()
    )
    shaderProgram.setUniformf(
      shaderProgram.getUniformLocation("u_regionBoundary"),
      renderCmp.sprite.u,
      renderCmp.sprite.v,
      renderCmp.sprite.u2,
      renderCmp.sprite.v2
    )

    // TODO provide commons "renderEntity" function that can be called here as well to avoid code duplication
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
}
