package com.github.quillraven.commons.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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

    LOG.debug { assetStorage.takeSnapshot().prettyPrint() }
    assetStorage.dispose()
  }

  companion object {
    val LOG = logger<AbstractGame>()
  }
}
