package com.github.quillraven.commons.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ScreenUtils
import com.github.quillraven.commons.ashley.system.RenderSystem
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.log.debug
import ktx.log.logger

/**
 * Implementation of [ShaderService] supporting following common [definitions][ShaderDefinition]:
 * - [ShaderDefinition.BLUR_SHADER]
 * - [ShaderDefinition.OUTLINE_SHADER]
 *
 * Stores any additional created resources in the [assetStorage] to dispose them when the game gets closed.
 *
 * Use [activeShader] to set the default [ShaderProgram] that will be used in the [RenderSystem].
 *
 * Use [ShaderDefinition.OUTLINE_SHADER] and [renderOutline] to render an outline of a [Sprite] in a specific color.
 *
 * Use [blurRadius] to apply a blur effect to the entire scene. Good values are between 3f and 6f.
 */
abstract class AbstractShaderService(
  private val assetStorage: AssetStorage,
  override val batch: Batch,
  override var activeShader: ShaderProgram = batch.shader
) : ShaderService {
  override var blurRadius = 0f

  // blur shader variables
  private var fbosLoaded = false
  private lateinit var frameBufferA: FrameBuffer
  private lateinit var frameBufferB: FrameBuffer
  private var directionLocation = 0
  private var radiusLocation = 0
  private var blurActive = false

  /**
   * Returns [ShaderProgram] for the given [definition]. If it is not loaded yet then
   * it will be loaded, compiled and stored in the [assetStorage] by the definition's id.
   */
  override fun shader(definition: ShaderDefinition): ShaderProgram {
    val shaderProgram = assetStorage.getOrNull<ShaderProgram>(definition.id)
    return if (shaderProgram != null) {
      // already loaded -> return it
      shaderProgram
    } else {
      // not loaded yet -> load it and check for errors
      val timeBefore = System.currentTimeMillis()

      val newShader = ShaderProgram(definition.vertexShader, definition.fragmentShader)
      if (!newShader.isCompiled) {
        throw GdxRuntimeException("Could not compile shader '${definition.id}': ${newShader.log}")
      }
      LOG.debug { "Needed '${System.currentTimeMillis() - timeBefore}' ms to load '${definition.id}' shader" }

      // store in assetStorage to dispose shader correctly
      KtxAsync.launch {
        assetStorage.add(definition.id, newShader)
      }

      if (definition == ShaderDefinition.BLUR_SHADER) {
        // blur shader loaded -> get uniform locations for faster uniform setting
        directionLocation = newShader.getUniformLocation("u_direction")
        radiusLocation = newShader.getUniformLocation("u_radius")
      }

      // return loaded shader
      newShader
    }
  }

  /**
   * Create or update the size of [frameBufferA] and [frameBufferB] with the given [width] and [height].
   * The create [FrameBuffer] instances are stored in the [assetStorage] to dispose them at the end of the game.
   */
  private fun createUpdateFbos(width: Int, height: Int) {
    if (!fbosLoaded) {
      // fbos not created yet -> create them
      LOG.debug { "Creating frame buffers for size: $width x $height" }
      fbosLoaded = true
      KtxAsync.launch {
        frameBufferA = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        frameBufferB = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        assetStorage.add("commonsShaderFboA", frameBufferA)
        assetStorage.add("commonsShaderFboB", frameBufferB)
      }
    } else if (frameBufferA.width != width || frameBufferA.height != height) {
      // fbos already created but size does not match -> dispose them and create new ones
      LOG.debug { "Resizing frame buffers to $width x $height" }
      KtxAsync.launch {
        // dispose existing fbos
        frameBufferA.dispose()
        frameBufferB.dispose()
        assetStorage.unload<FrameBuffer>("commonsShaderFboA")
        assetStorage.unload<FrameBuffer>("commonsShaderFboB")

        // create new fbos
        frameBufferA = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        frameBufferB = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
        assetStorage.add("commonsShaderFboA", frameBufferA)
        assetStorage.add("commonsShaderFboB", frameBufferB)
      }
    }
  }

  /**
   * Called before any rendering is done by the [RenderSystem].
   * If a [blurRadius] > 0 is specified then the [ShaderDefinition.BLUR_SHADER] will be loaded
   * and a blur effect is applied to the render scene.
   */
  override fun preRender() {
    if (blurRadius > 0f) {
      blurActive = true

      // blur specified -> render to FrameBuffer A to apply blur effect in postRender
      createUpdateFbos(Gdx.graphics.width, Gdx.graphics.height)
      frameBufferA.bind()
      ScreenUtils.clear(0f, 0f, 0f, 0f, false)
    }
  }

  /**
   * Renders a [sprite]'s outline with the given [outlineColor].
   * Requires that the [batch]'s shader is set to the [ShaderDefinition.OUTLINE_SHADER].
   */
  fun renderOutline(outlineColor: Color, sprite: Sprite) {
    with(sprite) {
      TMP_COLOR.set(color)
      color = outlineColor
      draw(batch, batch.color.a)
      color = TMP_COLOR
    }
  }

  /**
   * Called after all rendering is done by the [RenderSystem].
   * If a [blurRadius] was specified in [preRender] then a two pass blur will be applied to the scene.
   * First a horizontal blur using the [blurRadius]. Second a vertical blur using the [blurRadius] again.
   */
  override fun postRender() {
    if (blurActive) {
      blurActive = false
      renderTextureBlurred(frameBufferA.colorBufferTexture, blurRadius)
    }
  }

  /**
   * Applies a two pass blur effect to the given [texture] with a blur radius of [blurRadius].
   * Calls [Batch.setColor] with the given [color] before the rendering process and restores the color at the end.
   */
  override fun renderTextureBlurred(texture: Texture, blurRadius: Float, color: Color) {
    // in case this method gets called separately without the RenderSystem then
    // we need to create the frame buffer first
    createUpdateFbos(Gdx.graphics.width, Gdx.graphics.height)

    // apply horizontal blur to texture by rendering it to FrameBuffer B
    frameBufferB.bind()
    val blurShader = shader(ShaderDefinition.BLUR_SHADER)

    // viewport is the entire screen since we render the frame buffer texture pixel perfect 1:1
    ScreenUtils.clear(0f, 0f, 0f, 1f, false)
    HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    TMP_COLOR.set(batch.color)

    // use identity matrix to render pixel perfect
    batch.color = color
    batch.use(batch.projectionMatrix.idt()) {
      batch.shader = blurShader.apply {
        setUniformf(directionLocation, 1f, 0f)
        setUniformf(radiusLocation, blurRadius)
      }

      batch.draw(texture, -1f, 1f, 2f, -2f)
    }

    // render to screen by applying vertical blur
    // viewport and matrix are the same as before. Therefore, no need to set them again
    FrameBuffer.unbind()
    ScreenUtils.clear(0f, 0f, 0f, 1f, false)
    batch.use {
      batch.shader.apply {
        setUniformf(directionLocation, 0f, 1f)
        setUniformf(radiusLocation, blurRadius)
      }

      batch.draw(frameBufferB.colorBufferTexture, -1f, 1f, 2f, -2f)
    }

    // reset shader and color
    batch.color = TMP_COLOR
    batch.shader = activeShader
  }

  companion object {
    private val LOG = logger<AbstractShaderService>()
    private val TMP_COLOR = Color()
  }
}
