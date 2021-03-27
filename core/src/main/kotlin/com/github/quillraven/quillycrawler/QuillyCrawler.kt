package com.github.quillraven.quillycrawler

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.commons.audio.QueueAudioService
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.quillycrawler.screen.StartUpScreen

class QuillyCrawler : AbstractGame() {
  override val audioService: AudioService = QueueAudioService(assetStorage)

  fun isDevMode() = "true" == System.getProperty("devMode", "false")

  override fun create() {
    if (isDevMode()) {
      Gdx.app.logLevel = Application.LOG_DEBUG
    }

    addScreen(StartUpScreen(this))
    setScreen<StartUpScreen>()
  }

  companion object {
    const val UNIT_SCALE = 1 / 16f // 16 pixels is one in game world unit
  }
}
