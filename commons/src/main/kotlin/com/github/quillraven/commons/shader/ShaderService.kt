package com.github.quillraven.commons.shader

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.log.debug
import ktx.log.logger

interface ShaderService {
  val batch: Batch
  var activeShader: ShaderProgram

  fun shader(definition: ShaderDefinition): ShaderProgram

  fun postRenderEntities(entities: ImmutableArray<Entity>)
}

class EmptyShaderService(
  override val batch: Batch,
  override var activeShader: ShaderProgram = batch.shader
) : ShaderService {
  override fun shader(definition: ShaderDefinition): ShaderProgram = activeShader

  override fun postRenderEntities(entities: ImmutableArray<Entity>) = Unit
}

abstract class AbstractShaderService(
  private val assetStorage: AssetStorage,
  override val batch: Batch,
  override var activeShader: ShaderProgram = batch.shader
) : ShaderService {
  override fun shader(definition: ShaderDefinition): ShaderProgram {
    val shaderProgram = assetStorage.getOrNull<ShaderProgram>(definition.id)
    return if (shaderProgram != null) {
      shaderProgram
    } else {
      val timeBefore = System.currentTimeMillis()

      val newShader = ShaderProgram(definition.vertexShader, definition.fragmentShader)
      if (!newShader.isCompiled) {
        throw GdxRuntimeException("Could not compile shader '${definition.id}': ${newShader.log}")
      }
      KtxAsync.launch {
        assetStorage.add(definition.id, newShader)
      }

      LOG.debug { "Needed '${System.currentTimeMillis() - timeBefore}' ms to load '${definition.id}' shader" }

      newShader
    }
  }

  companion object {
    private val LOG = logger<AbstractShaderService>()
  }
}
