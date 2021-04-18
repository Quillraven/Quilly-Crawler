package com.github.quillraven.commons.game

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.commons.audio.AudioService
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage

/**
 * Abstract implementation of [KtxScreen]. It is used by [AbstractGame] and provides access to
 * generally used classes like [batch], [stage], [assetStorage] and [audioService].
 *
 * Calls [Stage.clear] when the screen's [hide] function is called.
 */
abstract class AbstractScreen(
  game: AbstractGame,
  val batch: Batch = game.batch,
  val stage: Stage = game.stage,
  val assetStorage: AssetStorage = game.assetStorage,
  val audioService: AudioService = game.audioService
) : KtxScreen {
  override fun hide() {
    stage.clear()
  }
}
