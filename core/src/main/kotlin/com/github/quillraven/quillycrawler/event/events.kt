package com.github.quillraven.quillycrawler.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.ReflectionPool
import com.github.quillraven.quillycrawler.ashley.component.DamageEmitterComponent
import com.github.quillraven.quillycrawler.combat.buff.Buff
import com.github.quillraven.quillycrawler.combat.command.Command
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.getOrPut
import kotlin.reflect.KClass

sealed class GameEvent : Pool.Poolable {
  override fun reset() = Unit
}

class MapChangeEvent : GameEvent() {
  lateinit var entity: Entity
  var level: Int = 0
}

class CombatCommandStarted : GameEvent() {
  lateinit var command: Command
}

class CombatPreDamageEvent : GameEvent() {
  lateinit var damageEmitterComponent: DamageEmitterComponent
}

class CombatPostDamageEvent : GameEvent() {
  lateinit var damageEmitterComponent: DamageEmitterComponent
}

class CombatBuffAdded : GameEvent() {
  lateinit var buff: Buff
}

class CombatBuffRemoved : GameEvent() {
  lateinit var buff: Buff
}

class CombatHealEvent : GameEvent() {
  lateinit var entity: Entity
  var amountLife = 0f
  var amountMana = 0f
}

object CombatVictoryEvent : GameEvent()

object CombatDefeatEvent : GameEvent()

class CombatStartEvent : GameEvent() {
  lateinit var playerEntity: Entity
}

class CombatNewTurnEvent : GameEvent() {
  var turn = 0
  val turnEntities = GdxArray<Entity>()

  override fun reset() {
    turnEntities.clear()
  }
}

class CombatDeathEvent : GameEvent() {
  lateinit var entity: Entity
}

class CombatCommandAddedEvent : GameEvent() {
  lateinit var command: Command
}

class CombatCommandPlayerEvent : GameEvent() {
  lateinit var command: Command
}

class CombatClearCommandsEvent : GameEvent() {
  lateinit var entity: Entity
}

interface GameEventListener {
  fun onEvent(event: GameEvent)
}

class GameEventDispatcher {
  @PublishedApi
  internal val eventPools = ObjectMap<KClass<out GameEvent>, ReflectionPool<out GameEvent>>()

  @PublishedApi
  internal val listeners = ObjectMap<KClass<out GameEvent>, GdxSet<GameEventListener>>()

  inline fun <reified T : GameEvent> addListener(listener: GameEventListener) {
    val eventListeners = listeners.getOrPut(T::class) { GdxSet() }
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

    listeners[T::class]?.forEach { it.onEvent(event) }
    pool.free(event)
  }
}
