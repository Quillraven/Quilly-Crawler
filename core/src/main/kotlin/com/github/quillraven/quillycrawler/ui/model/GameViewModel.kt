package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.quillycrawler.event.GameEvent
import com.github.quillraven.quillycrawler.event.GameEventListener
import com.github.quillraven.quillycrawler.event.MapChangeEvent
import ktx.collections.GdxSet

interface GameListener {
  fun onMapChange(mapName: StringBuilder) = Unit
}

data class GameViewModel(val bundle: I18NBundle) : GameEventListener {
  private val listeners = GdxSet<GameListener>()
  private val mapNameBuilder = StringBuilder()

  fun addGameListener(listener: GameListener) = listeners.add(listener)

  fun removeGameListener(listener: GameListener) = listeners.remove(listener)

  override fun onEvent(event: GameEvent) {
    if (event is MapChangeEvent) {
      mapNameBuilder.clear()
      mapNameBuilder.append(bundle["GameView.dungeonLevel"]).append(" ").append(event.level)
      listeners.forEach { it.onMapChange(mapNameBuilder) }
    }
  }
}
