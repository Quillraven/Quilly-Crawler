package com.github.quillraven.quillycrawler.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.IntMap
import ktx.collections.GdxSet
import ktx.collections.getOrPut

enum class GameEventType {
  MAP_CHANGE
}

sealed class GameEvent(val type: GameEventType)
data class MapChangeEvent(val entity: Entity, val level: Int) : GameEvent(GameEventType.MAP_CHANGE)

interface GameEventListener {
  fun onEvent(event: GameEvent)
}

class GameEventDispatcher {
  private val listeners = IntMap<GdxSet<GameEventListener>>()

  fun addListener(type: GameEventType, listener: GameEventListener) {
    val eventListeners = listeners.getOrPut(type.ordinal) { GdxSet() }
    eventListeners.add(listener)
  }

  fun removeListener(listener: GameEventListener) {
    listeners.values().forEach { it.remove(listener) }
  }

  fun dispatchEvent(event: GameEvent) {
    listeners[event.type.ordinal]?.forEach { it.onEvent(event) }
  }
}
