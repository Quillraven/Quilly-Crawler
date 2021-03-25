package com.github.quillraven.quillycrawler

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.commons.audio.QueueAudioService
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.screen.GameScreen
import com.github.quillraven.quillycrawler.ui.configureSkin

class QuillyCrawler : AbstractGame() {
  val audioService: AudioService = QueueAudioService(assetStorage, 2, 3)

  fun isDevMode() = "true" == System.getProperty("devMode", "false")

  override fun create() {
    if (isDevMode()) {
      Gdx.app.logLevel = Application.LOG_DEBUG
    }

    assetStorage.loadSync(I18NAssets.DEFAULT.descriptor)
    configureSkin(assetStorage)

    addScreen(GameScreen(this))
    setScreen<GameScreen>()
  }

  override fun pause() {
    audioService.pauseMusic()
    super.pause()
  }

  override fun resume() {
    audioService.resumeMusic()
    super.resume()
  }

  override fun render() {
    super.render()
    audioService.update(Gdx.graphics.deltaTime)
  }

  companion object {
    const val UNIT_SCALE = 1 / 16f // 16 pixels is one in game world unit
  }
}
