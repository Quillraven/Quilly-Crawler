package com.github.quillraven.quillycrawler.shader

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g2d.Batch
import com.github.quillraven.commons.shader.AbstractShaderService
import com.github.quillraven.commons.shader.ShaderDefinition
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.allOf
import ktx.assets.async.AssetStorage

class DefaultShaderService(
  assetStorage: AssetStorage,
  batch: Batch,
  engine: Engine
) : AbstractShaderService(assetStorage, batch) {
  private val interactPlayerEntities =
    engine.getEntitiesFor(allOf(PlayerComponent::class, InteractComponent::class).get())

  private val outlineShader = shader(ShaderDefinition.OUTLINE_SHADER)

  override fun postRenderEntities(entities: ImmutableArray<Entity>) {
    // draw outlines for actionable entities
    interactPlayerEntities.forEach { player ->
      val entitiesInRange = player.interactCmp.entitiesInRange
      if (entitiesInRange.isEmpty) {
        return@forEach
      }

      // switch to outline shader if it is not set yet
      if (batch.shader != outlineShader) {
        batch.shader = outlineShader
      }

      entitiesInRange.forEach { renderEntityOutline(it.actionableCmp.outlineColor, it) }
    }

    if (batch.shader != activeShader) {
      // reset to previously active shader if necessary
      batch.shader = activeShader
    }
  }
}
