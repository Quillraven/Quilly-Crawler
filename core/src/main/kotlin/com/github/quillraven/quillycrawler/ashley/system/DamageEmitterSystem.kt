package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.ashley.component.shake
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.command.CommandDeath
import com.github.quillraven.quillycrawler.event.CombatDamageEvent
import com.github.quillraven.quillycrawler.event.CombatDeathEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.logger

class DamageEmitterSystem(private val gameEventDispatcher: GameEventDispatcher) :
  IteratingSystem(allOf(DamageEmitterComponent::class).exclude(RemoveComponent::class).get()) {
  private val damageEvent = CombatDamageEvent(DamageEmitterComponent())
  private val deathEvent = CombatDeathEvent()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val damageEmitterCmp = entity.damageEmitterCmp

    if (damageEmitterCmp.damageDelay <= 0f) {
      // deal damage
      // dispatch event so that the target could react e.g. via a buff that reduces the incoming damage by
      // modifying the emitter data
      val physicalDamageBefore = damageEmitterCmp.physicalDamage
      val magicalDamageBefore = damageEmitterCmp.magicDamage
      gameEventDispatcher.dispatchEvent(damageEvent.apply { this.damageEmitterComponent = damageEmitterCmp })
      val targetStatsCmp = damageEmitterCmp.target.statsCmp

      var targetLife = targetStatsCmp[StatsType.LIFE]
      val physicalReduction = targetStatsCmp[StatsType.PHYSICAL_ARMOR].coerceAtMost(100f) * 0.01f
      val magicalReduction = targetStatsCmp[StatsType.MAGIC_ARMOR].coerceAtMost(100f) * 0.01f
      targetLife -= damageEmitterCmp.physicalDamage * (1f - physicalReduction)
      targetLife -= damageEmitterCmp.magicDamage * (1f - magicalReduction)

      LOG.debug {
        """${damageEmitterCmp.target}'s life changed to $targetLife:
        |physicalBefore=$physicalDamageBefore,
        |magicalBefore=$magicalDamageBefore,
        |physicalAfter=${damageEmitterCmp.physicalDamage},
        |magicalAfter=${damageEmitterCmp.magicDamage}""".trimMargin().replace("\n", "")
      }

      if (targetLife < targetStatsCmp[StatsType.LIFE]) {
        // entity took damage -> shake it
        damageEmitterCmp.target.shake(engine, 0.2f, 0.5f)
      }

      targetStatsCmp[StatsType.LIFE] = targetLife
      if (targetLife <= 0f) {
        LOG.debug { "Entity ${damageEmitterCmp.target} died" }
        damageEmitterCmp.target[CombatAIComponent.MAPPER]?.behaviorTree?.step()
        if (hasDeathCommand(damageEmitterCmp.target.combatCmp) || damageEmitterCmp.target.isPlayer) {
          // target really died or was a player entity
          gameEventDispatcher.dispatchEvent(deathEvent.apply { this.entity = damageEmitterCmp.target })
        }
      }

      // remove damage emitter
      entity.removeFromEngine(engine)
    } else {
      damageEmitterCmp.damageDelay -= deltaTime
    }
  }

  private fun hasDeathCommand(combatCmp: CombatComponent): Boolean {
    combatCmp.commandsToExecute.forEach {
      if (it is CommandDeath) {
        return true
      }
    }
    return false
  }

  companion object {
    private val LOG = logger<DamageEmitterSystem>()
  }
}
