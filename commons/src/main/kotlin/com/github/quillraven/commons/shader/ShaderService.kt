package com.github.quillraven.commons.shader

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.github.quillraven.commons.ashley.system.RenderSystem

/**
 * Interface to apply special [shaders][ShaderProgram] to the render process.
 * A [ShaderService] is used by the [RenderSystem] and calls its functions accordingly.
 */
interface ShaderService {
  val batch: Batch
  var activeShader: ShaderProgram
  var blurRadius: Float

  fun shader(definition: ShaderDefinition): ShaderProgram

  fun preRender()

  fun postRenderEntities(entities: ImmutableArray<Entity>)

  fun postRender()
}

/**
 * Empty implementation of [ShaderService]. Can be used as default value to avoid null services.
 */
class DefaultShaderService(
  override val batch: Batch,
  override var activeShader: ShaderProgram = batch.shader,
  override var blurRadius: Float = 0f
) : ShaderService {
  override fun shader(definition: ShaderDefinition): ShaderProgram = activeShader

  override fun preRender() = Unit

  override fun postRenderEntities(entities: ImmutableArray<Entity>) = Unit

  override fun postRender() = Unit
}
