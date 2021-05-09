package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Constructor
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.BuffComponent
import com.github.quillraven.quillycrawler.ashley.component.buffCmp
import com.github.quillraven.quillycrawler.combat.buff.Buff
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.GameEventType
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.collections.getOrPut
import ktx.collections.iterate
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
) : IteratingSystem(allOf(BuffComponent::class).exclude(RemoveComponent::class).get()) {
  private val buffPools = ObjectMap<KClass<out Buff>, BuffPool<out Buff>>()

  private fun updateEntityBuff(entity: Entity, buffCmp: BuffComponent, buffType: KClass<out Buff>) {
    if (buffType == Buff::class) {
      LOG.error { "Buff not specified. Will not update it!" }
      return
    }

    // check if entity already has buff of given type -> if yes then just reset the already existing buff
    val currentBuff = buffCmp.buffs[buffType]
    if (currentBuff != null) {
      LOG.debug { "Entity already has buff of type ${buffType.simpleName}" }
      currentBuff.reset()
      return
    }

    // entity does not have buff yet -> create it
    val newBuff = buffPools.getOrPut(buffType) {
      try {
        val constructor = ClassReflection.getConstructor(buffType.java, Engine::class.java, AudioService::class.java)
        BuffPool(constructor, engine, audioService)
      } catch (e: ReflectionException) {
        throw GdxRuntimeException("Could not find (Engine, AudioService) constructor for buff ${buffType.simpleName}")
      }
    }.obtain()
    newBuff.target = entity
    gameEventDispatcher.addListener(GameEventType.DAMAGE, newBuff)
    newBuff.onAdd()
    // add new buff to entity buff map to avoid adding the same buff multiple times
    buffCmp.buffs[buffType] = newBuff
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    with(entity.buffCmp) {
      // update buffs
      buffsToAdd.iterate { buffType, iterator ->
        updateEntityBuff(entity, this, buffType)
        iterator.remove()
      }

      buffs.iterate { buffType, buff, iterator ->
        if (buff.isFinished()) {
          LOG.debug { "Removing buff ${buffType.simpleName}" }
          // remove finished buffs
          gameEventDispatcher.removeListener(buff)
          buff.onRemove()
          @Suppress("UNCHECKED_CAST")
          val pool = buffPools.get(buffType) as BuffPool<Buff>
          pool.free(buff)
          iterator.remove()
        }
      }
    }
  }

  companion object {
    private val LOG = logger<BuffSystem>()
  }
}
