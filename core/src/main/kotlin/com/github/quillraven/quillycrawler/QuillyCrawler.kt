package com.github.quillraven.quillycrawler

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.quillycrawler.screen.PlayGroundScreen

class QuillyCrawler : AbstractGame() {
  fun isDevMode() = "true" == System.getProperty("devMode", "false")

  override fun create() {
    if (isDevMode()) {
      Gdx.app.logLevel = Application.LOG_DEBUG
    }

    addScreen(PlayGroundScreen(this))
    setScreen<PlayGroundScreen>()
  }

  companion object {
    const val UNIT_SCALE = 1 / 16f // 16 pixels is one in game world unit
  }
}
