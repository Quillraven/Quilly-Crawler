package com.github.quillraven.commons.shader

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.renderCmp
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.log.debug
import ktx.log.logger

interface ShaderService {
  val batch: Batch
  var activeShader: ShaderProgram
  var blurRadius: Float

  fun shader(definition: ShaderDefinition): ShaderProgram

  fun preRender(viewport: Viewport)

  fun postRenderEntities(entities: ImmutableArray<Entity>)

  fun postRender(viewport: Viewport)
}

class EmptyShaderService(
  override val batch: Batch,
  override var activeShader: ShaderProgram = batch.shader,
  override var blurRadius: Float = 0f
) : ShaderService {
  override fun shader(definition: ShaderDefinition): ShaderProgram = activeShader

  override fun preRender(viewport: Viewport) = Unit

  override fun postRenderEntities(entities: ImmutableArray<Entity>) = Unit

  override fun postRender(viewport: Viewport) = Unit
}

abstract class AbstractShaderService(
  private val assetStorage: AssetStorage,
  override val batch: Batch,
  override var activeShader: ShaderProgram = batch.shader
) : ShaderService {
  override var blurRadius = 0f

  private var fbosLoaded = false
  private lateinit var frameBufferA: FrameBuffer
  private lateinit var frameBufferB: FrameBuffer
  private var directionLocation = 0
  private var radiusLocation = 0

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

      if (definition == ShaderDefinition.BLUR_SHADER) {
        directionLocation = newShader.getUniformLocation("u_direction")
        radiusLocation = newShader.getUniformLocation("u_radius")
      }

      newShader
    }
  }

  private fun createUpdateFbos(width: Int, height: Int) {
    if (!fbosLoaded) {
      LOG.debug { "Creating frame buffers for size: $width x $height" }
      fbosLoaded = true
      KtxAsync.launch {
        frameBufferA = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        frameBufferB = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        assetStorage.add("commonsShaderFboA", frameBufferA)
        assetStorage.add("commonsShaderFboB", frameBufferB)
      }
    } else if (frameBufferA.width != width || frameBufferA.height != height) {
      LOG.debug { "Resizing frame buffers to $width x $height" }
      KtxAsync.launch {
        frameBufferA.dispose()
        frameBufferB.dispose()
        assetStorage.unload<FrameBuffer>("commonsShaderFboA")
        assetStorage.unload<FrameBuffer>("commonsShaderFboB")
        frameBufferA = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        frameBufferB = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        assetStorage.add("commonsShaderFboA", frameBufferA)
        assetStorage.add("commonsShaderFboB", frameBufferB)
      }
    }
  }

  override fun preRender(viewport: Viewport) {
    if (blurRadius > 0f) {
      // blur specified -> render to FrameBuffer A to apply blur effect in postRender
      val width = viewport.screenWidth
      val height = viewport.screenHeight
      createUpdateFbos(width, height)

      frameBufferA.bind()
      ScreenUtils.clear(0f, 0f, 0f, 0f, false)
      Gdx.gl20.glViewport(0, 0, viewport.screenWidth, viewport.screenHeight)
    }
  }

  fun renderEntityOutline(outlineColor: Color, entity: Entity) {
    with(entity.renderCmp.sprite) {
      TMP_COLOR.set(color)
      color = outlineColor
      draw(batch, batch.color.a)
      color = TMP_COLOR
    }
  }

  override fun postRender(viewport: Viewport) {
    if (blurRadius > 0f) {
      // blur was specified -> apply horizontal blur to FrameBuffer A by rendering it into FrameBuffer B
      frameBufferB.bind()
      ScreenUtils.clear(0f, 0f, 0f, 1f, false)
      Gdx.gl20.glViewport(0, 0, viewport.screenWidth, viewport.screenHeight)
      batch.use(batch.projectionMatrix.idt()) {
        batch.shader = shader(ShaderDefinition.BLUR_SHADER).apply {
          setUniformf(directionLocation, 1f, 0f)
          setUniformf(radiusLocation, blurRadius)
        }

        batch.draw(frameBufferA.colorBufferTexture, -1f, 1f, 2f, -2f)
      }

      // render to screen by applying vertical blur
      FrameBuffer.unbind()
      ScreenUtils.clear(0f, 0f, 0f, 1f, false)
      Gdx.gl20.glViewport(viewport.screenX, viewport.screenY, viewport.screenWidth, viewport.screenHeight)
      batch.use(batch.projectionMatrix.idt()) {
        batch.shader = shader(ShaderDefinition.BLUR_SHADER).apply {
          setUniformf(directionLocation, 0f, 1f)
          setUniformf(radiusLocation, blurRadius)
        }

        batch.draw(frameBufferB.colorBufferTexture, -1f, 1f, 2f, -2f)
      }
    }
  }

  companion object {
    private val LOG = logger<AbstractShaderService>()
    private val TMP_COLOR = Color()
  }
}
