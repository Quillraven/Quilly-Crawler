package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.DamageEmitterComponent
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.ashley.component.totalStatValue
import com.github.quillraven.quillycrawler.assets.SoundAssets
import ktx.ashley.entity
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.log.debug
import ktx.log.logger

class CombatOrder(
  private val engine: Engine,
  private val audioService: AudioService
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
      resetAnimation()
      true
    } else {
      false
    }
  }

  fun playSound(sound: SoundAssets) {
    audioService.playSound(sound.descriptor.fileName)
  }

  fun playAnimation(stateKey: String, animationSpeed: Float = 1f) {
    with(source.animationCmp) {
      this.playMode = Animation.PlayMode.NORMAL
      if (this.stateKey == stateKey) {
        this.stateTime = 0f
      } else {
        this.stateKey = stateKey
      }
      this.animationSpeed = animationSpeed
    }
  }

  fun resetAnimation() {
    with(source.animationCmp) {
      this.stateKey = "idle"
      this.animationSpeed = 0f
      this.stateTime = 0f
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

  fun killEntity() {
    source.removeFromEngine(engine)
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
