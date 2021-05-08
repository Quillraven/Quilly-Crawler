package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Constructor
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.BuffComponent
import com.github.quillraven.quillycrawler.ashley.component.buffCmp
import com.github.quillraven.quillycrawler.combat.buff.Buff
import com.github.quillraven.quillycrawler.event.GameEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.GameEventListener
import com.github.quillraven.quillycrawler.event.GameEventType
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.collections.getOrPut
import ktx.collections.isNotEmpty
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger
import kotlin.reflect.KClass

private class BuffPool<T : Buff>(
  private val constructor: Constructor,
  private val engine: Engine,
  private val audioService: AudioService
) : Pool<T>() {
  override fun newObject(): T {
    @Suppress("UNCHECKED_CAST")
    return constructor.newInstance(engine, audioService) as T
  }
}

class BuffSystem(
  private val gameEventDispatcher: GameEventDispatcher,
  private val audioService: AudioService,
) : GameEventListener, IteratingSystem(allOf(BuffComponent::class).exclude(RemoveComponent::class).get()),
  EntityListener {
  private val buffPools = ObjectMap<KClass<out Buff>, BuffPool<out Buff>>()
  private val entityBuffs = ObjectMap<Entity, ObjectMap<KClass<out Buff>, Buff>>()

  init {
    gameEventDispatcher.addListener(GameEventType.COMBAT_DEFEAT, this)
    gameEventDispatcher.addListener(GameEventType.COMBAT_VICTORY, this)
  }

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    engine.addEntityListener(family, this)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
  }

  override fun entityAdded(buffEntity: Entity) {
    with(buffEntity.buffCmp) {
      if (buffType == Buff::class) {
        LOG.error { "Buff not specified for entity $buffEntity" }
        return
      }

      // check if entity already has buff of given type -> if yes then just reset the already existing buff
      val currentEntityBuffs = entityBuffs.getOrPut(this.entity) { ObjectMap() }
      val currentBuff = currentEntityBuffs.get(buffType)
      if (currentBuff != null) {
        LOG.debug { "$buffEntity already has buff of type ${buffType.simpleName}" }
        // remove new entity and set buffType to Buff to avoid lateinit exception in entityRemoved function
        buffType = Buff::class
        buffEntity.removeFromEngine(engine)
        // reset existing buff
        currentBuff.reset()
        return
      }

      // entity does not have buff yet -> create it
      buff = buffPools.getOrPut(buffType) {
        try {
          val constructor = ClassReflection.getConstructor(buffType.java, Engine::class.java, AudioService::class.java)
          BuffPool(constructor, engine, audioService)
        } catch (e: ReflectionException) {
          throw GdxRuntimeException("Could not find (Engine, AudioService) constructor for buff $buffType")
        }
      }.obtain()
      buff.target = this.entity
      gameEventDispatcher.addListener(GameEventType.DAMAGE, buff)
      buff.onAdd()
      // add new buff to entity buff map to avoid adding the same buff multiple times
      currentEntityBuffs[buffType] = buff
    }
  }

  override fun entityRemoved(buffEntity: Entity) {
    with(buffEntity.buffCmp) {
      if (buffType == Buff::class) {
        // this case can happen when a buff entity gets removed which does not have a created buff instance.
        // this happens when the target entity already had this bufftype in the entityAdded function
        return
      }

      if (entityBuffs.isNotEmpty()) {
        // we need to check for isNotEmpty because the map gets cleared when the combat is over (=DEFEAT/VICTORY event).
        // otherwise, the access via entity will throw an exception
        entityBuffs.get(this.entity).remove(buffType)
      }
      gameEventDispatcher.removeListener(buff)
      buff.onRemove()
      @Suppress("UNCHECKED_CAST")
      val pool = buffPools.get(buffType) as BuffPool<Buff>
      pool.free(buff)
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (entity.buffCmp.buff.isFinished()) {
      entity.removeFromEngine(engine)
    }
  }

  override fun onEvent(event: GameEvent) {
    entityBuffs.clear()
  }

  companion object {
    private val LOG = logger<BuffSystem>()
  }
}
