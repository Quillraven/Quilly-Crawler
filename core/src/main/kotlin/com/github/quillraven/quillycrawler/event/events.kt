package com.github.quillraven.quillycrawler.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ReflectionPool
import com.github.quillraven.quillycrawler.ashley.component.DamageEmitterComponent
import com.github.quillraven.quillycrawler.combat.command.Command
import ktx.collections.GdxSet
import ktx.collections.getOrPut
import kotlin.reflect.KClass

enum class GameEventType {
  MAP_CHANGE,
  DAMAGE,
  DEATH,
  COMBAT_VICTORY,
  COMBAT_DEFEAT,
  PLAYER_TURN,
  COMBAT_COMMAND_ADDED,
  COMBAT_COMMAND_PLAYER,
}

sealed class GameEvent(val type: GameEventType)

class MapChangeEvent : GameEvent(GameEventType.MAP_CHANGE) {
  lateinit var entity: Entity
  var level: Int = 0
}

class CombatDamageEvent : GameEvent(GameEventType.DAMAGE) {
  lateinit var damageEmitterComponent: DamageEmitterComponent
}

class CombatVictoryEvent : GameEvent(GameEventType.COMBAT_VICTORY)

class CombatDefeatEvent : GameEvent(GameEventType.COMBAT_DEFEAT)

class CombatPlayerTurnEvent : GameEvent(GameEventType.PLAYER_TURN)

class CombatDeathEvent : GameEvent(GameEventType.DEATH) {
  lateinit var entity: Entity
}

class CombatCommandAddedEvent : GameEvent(GameEventType.COMBAT_COMMAND_ADDED) {
  lateinit var command: Command
}

class CombatCommandPlayer : GameEvent(GameEventType.COMBAT_COMMAND_PLAYER) {
  lateinit var command: Command
}

interface GameEventListener {
  fun onEvent(event: GameEvent)
}

class GameEventDispatcher {
  @PublishedApi
  internal val eventPools = ObjectMap<KClass<out GameEvent>, ReflectionPool<out GameEvent>>()

  @PublishedApi
  internal val listeners = IntMap<GdxSet<GameEventListener>>()

  fun addListener(type: GameEventType, listener: GameEventListener) {
    val eventListeners = listeners.getOrPut(type.ordinal) { GdxSet() }
    eventListeners.add(listener)
  }

  fun removeListener(listener: GameEventListener) {
    listeners.values().forEach { it.remove(listener) }
  }

  inline fun <reified T : GameEvent> dispatchEvent(action: T.() -> Unit = {}) {
    @Suppress("UNCHECKED_CAST")
    val pool = eventPools.getOrPut(T::class) { ReflectionPool(T::class.java) } as ReflectionPool<T>
    val event = pool.obtain() as T
    event.apply(action)

    listeners[event.type.ordinal]?.forEach { it.onEvent(event) }
    pool.free(event)
  }
}
