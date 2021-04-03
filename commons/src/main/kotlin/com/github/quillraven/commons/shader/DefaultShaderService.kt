package com.github.quillraven.commons.shader

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.log.error
import ktx.log.logger
import kotlin.system.measureTimeMillis

class DefaultShaderService(private val assetStorage: AssetStorage) : ShaderService {
  private val defaultShaderProgram: ShaderProgram

  init {
    val timeNeeded = measureTimeMillis {
      SUPPORTED_SHADERS.forEach { shader ->
        val shaderProgram = ShaderProgram(shader.vertexShader, shader.fragmentShader)
        if (!shaderProgram.isCompiled) {
          throw GdxRuntimeException("Could not compile shader '$shader': ${shaderProgram.log}")
        }
        KtxAsync.launch {
          assetStorage.add(shader.id, shaderProgram)
        }
      }
    }
    LOG.debug { "Needed '$timeNeeded' ms to load '${SUPPORTED_SHADERS.size}' shaders" }

    defaultShaderProgram = assetStorage.get(Shader.DEFAULT_SHADER.id)
  }

  override fun shader(shader: Shader): ShaderProgram {
    val shaderProgram = assetStorage.getOrNull<ShaderProgram>(shader.id)
    return if (shaderProgram != null) {
      shaderProgram
    } else {
      LOG.error { "Could not get shader '${shader.id}'. Was it loaded already?" }
      defaultShaderProgram
    }
  }

  companion object {
    private val LOG = logger<DefaultShaderService>()
    private val SUPPORTED_SHADERS = gdxArrayOf(Shader.DEFAULT_SHADER, Shader.OUTLINE_SHADER)
  }
}
