package com.github.quillraven.quillycrawler.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.IntMap
import com.github.quillraven.quillycrawler.ashley.component.DamageEmitterComponent
import ktx.collections.GdxSet
import ktx.collections.getOrPut

enum class GameEventType {
  MAP_CHANGE,
  DAMAGE,
  DEATH,
  COMBAT_VICTORY,
  COMBAT_DEFEAT,
  PLAYER_TURN
}

sealed class GameEvent(val type: GameEventType)
data class MapChangeEvent(val entity: Entity, val level: Int) : GameEvent(GameEventType.MAP_CHANGE)
data class CombatDamageEvent(var damageEmitterComponent: DamageEmitterComponent) : GameEvent(GameEventType.DAMAGE)
class CombatVictoryEvent : GameEvent(GameEventType.COMBAT_VICTORY)
class CombatDefeatEvent : GameEvent(GameEventType.COMBAT_DEFEAT)
class CombatPlayerTurnEvent : GameEvent(GameEventType.PLAYER_TURN)
class CombatDeathEvent : GameEvent(GameEventType.DEATH) {
  lateinit var entity: Entity
}

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
