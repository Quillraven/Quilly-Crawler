package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.buff.Buff
import com.github.quillraven.quillycrawler.combat.effect.CombatOrderEffect
import com.github.quillraven.quillycrawler.combat.effect.CombatOrderEffectUndefined
import ktx.ashley.entity
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.log.debug
import ktx.log.logger
import kotlin.reflect.KClass

class CombatOrder(
  val engine: Engine,
  val audioService: AudioService
) : Pool.Poolable {
  lateinit var source: Entity
  var effect: CombatOrderEffect = CombatOrderEffectUndefined
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
          this.source = this@CombatOrder.source
          this.target = targetEntity
          this.physicalDamage = this@CombatOrder.source.totalStatValue(StatsType.PHYSICAL_DAMAGE)
        }
      }
    }
  }

  fun addBuff(buffType: KClass<out Buff>) {
    engine.entity {
      with<BuffComponent> {
        this.buffType = buffType
        this.entity = source
      }
    }
  }

  override fun reset() {
    totalTime = 0f
    targets.clear()
    effect.reset()
  }

  companion object {
    private val LOG = logger<CombatOrder>()
  }
}
