package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.DamageEmitterComponent
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.ashley.component.totalStatValue
import com.github.quillraven.quillycrawler.combat.effect.CommandEffect
import com.github.quillraven.quillycrawler.combat.effect.CommandEffectUndefined
import ktx.ashley.entity
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.log.debug
import ktx.log.logger

class Command(
  val engine: Engine,
  val audioService: AudioService
) : Pool.Poolable {
  lateinit var source: Entity
  var effect: CommandEffect = CommandEffectUndefined
  var targets = GdxArray<Entity>()
  var totalTime = 0f

  fun update(deltaTime: Float): Boolean {
    if (totalTime == 0f) {
      LOG.debug { "Executing effect ${effect::class.simpleName} for entity $source" }
      effect.start(this)
    }

    totalTime += deltaTime
    return if (effect.update(this, deltaTime)) {
      source.statsCmp[StatsType.MANA] = source.statsCmp[StatsType.MANA] - effect.manaCost
      effect.end(this)
      source.playAnimation("idle", 0f)
      true
    } else {
      false
    }
  }

  fun dealAttackDamage() {
    targets.forEach { targetEntity ->
      engine.entity {
        with<DamageEmitterComponent> {
          this.source = this@Command.source
          this.target = targetEntity
          this.physicalDamage = this@Command.source.totalStatValue(StatsType.PHYSICAL_DAMAGE)
        }
      }
    }
  }

  override fun reset() {
    totalTime = 0f
    targets.clear()
    effect.reset()
  }

  companion object {
    private val LOG = logger<Command>()
  }
}
