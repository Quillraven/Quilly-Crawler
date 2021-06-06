package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.quillycrawler.ashley.component.BuffComponent
import com.github.quillraven.quillycrawler.ashley.component.buffCmp
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.buff.Buff
import com.github.quillraven.quillycrawler.combat.buff.BuffPools
import com.github.quillraven.quillycrawler.event.CombatBuffAdded
import com.github.quillraven.quillycrawler.event.CombatBuffRemoved
import com.github.quillraven.quillycrawler.event.CombatPreDamageEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.collections.iterate
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger
import kotlin.reflect.KClass

class BuffSystem(
  combatContext: CombatContext,
  private val gameEventDispatcher: GameEventDispatcher,
) : IteratingSystem(allOf(BuffComponent::class).exclude(RemoveComponent::class).get()) {
  private val buffPools = BuffPools(combatContext)

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
    val newBuff = buffPools.obtainBuff(buffType) {
      this.entity = entity
    }
    gameEventDispatcher.addListener<CombatPreDamageEvent>(newBuff)
    newBuff.onAdd()
    // add new buff to entity buff map to avoid adding the same buff multiple times
    buffCmp.buffs[buffType] = newBuff

    gameEventDispatcher.dispatchEvent<CombatBuffAdded> { this.buff = newBuff }
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
          iterator.remove()
          gameEventDispatcher.dispatchEvent<CombatBuffRemoved> { this.buff = buff }
          buffPools.freeBuff(buff)
        }
      }
    }
  }

  companion object {
    private val LOG = logger<BuffSystem>()
  }
}
