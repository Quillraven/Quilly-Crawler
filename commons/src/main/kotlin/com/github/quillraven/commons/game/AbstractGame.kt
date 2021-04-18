package com.github.quillraven.commons.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.audio.AudioService
import ktx.app.KtxGame
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.log.debug
import ktx.log.info
import ktx.log.logger

/**
 * Abstract implementation of [KtxGame] using a [SpriteBatch] as a [batch] and an [assetStorage] for
 * loading and unloading assets.
 *
 * When the game's [pause] method is called then [render] will do nothing until [resume] is called. This happens
 * e.g. when the window gets minimized. Will also call the [audioService] pause, resume and update method accordingly.
 *
 * Creates a [stage] for the [uiViewport] which is used for Scene2D UI stuff. The [stage] gets updated and
 * rendered at the end of each [render] call.
 *
 * Prints debug information of the [batch] and the [assetStorage] when its [dispose] method is called and the
 * log level is set to [Application.LOG_DEBUG].
 *
 * Any [Screen] of the [KtxGame] must implement the [AbstractScreen] class.
 */
abstract class AbstractGame : KtxGame<AbstractScreen>() {
  val batch: Batch by lazy { SpriteBatch() }
  val assetStorage by lazy {
    KtxAsync.initiate()
    AssetStorage()
  }
  abstract val audioService: AudioService
  abstract val uiViewport: Viewport
  val stage: Stage by lazy { Stage(uiViewport, batch) }
  private var isPaused = false

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    uiViewport.update(width, height, true)
  }

  override fun pause() {
    isPaused = true
    audioService.pause()
    super.pause()
  }

  override fun resume() {
    isPaused = false
    audioService.resume()
    super.resume()
  }

  override fun render() {
    if (isPaused) {
      return
    }

    super.render()
    with(stage) {
      act(Gdx.graphics.deltaTime)
      uiViewport.apply()
      draw()
    }
    audioService.update()
  }

  override fun dispose() {
    super.dispose()

    if (Gdx.app.logLevel == Application.LOG_DEBUG) {
      if (batch is SpriteBatch) {
        val spriteBatch = batch as SpriteBatch
        LOG.debug { "Max sprites in batch: '${spriteBatch.maxSpritesInBatch}'" }
        LOG.debug { "Previous renderCalls: '${spriteBatch.renderCalls}'" }
      } else {
        LOG.info { "Batch is not of type SpriteBatch. Debug performance logging is skipped!" }
      }
    }
    batch.dispose()
    stage.dispose()

    LOG.debug { assetStorage.takeSnapshot().prettyPrint() }
    assetStorage.dispose()
  }

  companion object {
    val LOG = logger<AbstractGame>()
  }
}
