package com.github.quillraven.quillycrawler

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.PropertiesUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.commons.audio.DefaultAudioService
import com.github.quillraven.commons.audio.QueueAudioService
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.quillycrawler.screen.StartUpScreen

class QuillyCrawler : AbstractGame() {
  override val uiViewport: Viewport = FitViewport(320f, 180f)
  override val audioService: AudioService by lazy {
    if (gameProperties.get("sound", "true").toBoolean()) {
      QueueAudioService(assetStorage)
    } else {
      DefaultAudioService
    }
  }
  private val gameProperties = ObjectMap<String, String>()

  fun b2dDebug(): Boolean = gameProperties.get("b2d-debug", "false").toBoolean()

  override fun create() {
    PropertiesUtils.load(gameProperties, assetStorage.fileResolver.resolve("game.properties").reader())

    Gdx.app.logLevel = gameProperties.get("log-level", "${Application.LOG_ERROR}").toInt()

    addScreen(StartUpScreen(this))
    setScreen<StartUpScreen>()
  }

  companion object {
    const val UNIT_SCALE = 1 / 16f // 16 pixels is one in game world unit
  }
}
