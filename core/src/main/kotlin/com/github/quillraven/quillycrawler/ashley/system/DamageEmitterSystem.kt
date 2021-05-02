package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.event.CombatDamageEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.logger

class DamageEmitterSystem(private val gameEventDispatcher: GameEventDispatcher) :
  IteratingSystem(allOf(DamageEmitterComponent::class).exclude(RemoveComponent::class).get()) {
  private val damageEvent = CombatDamageEvent(DamageEmitterComponent())

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val damageEmitterCmp = entity.damageEmitterCmp

    if (damageEmitterCmp.damageDelay <= 0f) {
      // deal damage
      // dispatch event so that the target could react e.g. via a buff that reduces the incoming damage by
      // modifying the emitter data
      gameEventDispatcher.dispatchEvent(damageEvent.apply { this.damageEmitterComponent = damageEmitterCmp })
      val targetStatsCmp = damageEmitterCmp.target.statsCmp
      //TODO reduce damage by armor
      var targetLife = targetStatsCmp[StatsType.LIFE]
      targetLife -= damageEmitterCmp.physicalDamage
      targetLife -= damageEmitterCmp.magicDamage

      LOG.debug { "${damageEmitterCmp.target}'s life changed to $targetLife" }
      targetStatsCmp[StatsType.LIFE] = targetLife
      if (targetLife <= 0f) {
        LOG.debug { "Entity ${damageEmitterCmp.target} died" }
        damageEmitterCmp.target[CombatAIComponent.MAPPER]?.behaviorTree?.step()
      }

      // remove damage emitter
      entity.removeFromEngine(engine)
    } else {
      damageEmitterCmp.damageDelay -= deltaTime
    }
  }

  companion object {
    private val LOG = logger<DamageEmitterSystem>()
  }
}
