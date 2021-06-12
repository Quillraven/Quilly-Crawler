package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.ashley.component.HealEmitterComponent
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.healEmitterCmp
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.event.CombatPostHealEvent
import com.github.quillraven.quillycrawler.event.CombatPreHealEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf
import ktx.ashley.exclude

class HealEmitterSystem(private val gameEventDispatcher: GameEventDispatcher) :
  IteratingSystem(allOf(HealEmitterComponent::class).exclude(RemoveComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val healEmitterCmp = entity.healEmitterCmp

    if (healEmitterCmp.delay <= 0f) {
      // heal
      // dispatch event so that the target could react e.g. via a buff that increases heal amount by
      // modifying the emitter data
      gameEventDispatcher.dispatchEvent<CombatPreHealEvent> { this.healEmitterComponent = healEmitterCmp }
      val targetStatsCmp = healEmitterCmp.target.statsCmp
      val life = targetStatsCmp[StatsType.LIFE]
      val mana = targetStatsCmp[StatsType.MANA]
      targetStatsCmp[StatsType.LIFE] = (life + healEmitterCmp.life).coerceAtMost(targetStatsCmp[StatsType.MAX_LIFE])
      targetStatsCmp[StatsType.MANA] = (mana + healEmitterCmp.mana).coerceAtMost(targetStatsCmp[StatsType.MAX_MANA])
      gameEventDispatcher.dispatchEvent<CombatPostHealEvent> { this.healEmitterComponent = healEmitterCmp }

      // remove heal emitter
      entity.removeFromEngine(engine)
    } else {
      healEmitterCmp.delay -= deltaTime
    }
  }
}
