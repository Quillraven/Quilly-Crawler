package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.audio.AudioService
import ktx.log.debug
import ktx.log.logger

class CombatOrder(val audioService: AudioService) : Pool.Poolable {
  lateinit var source: Entity
  var effect: CombatOrderEffect = CombatOrderEffectUndefined
  var totalTime = 0f

  fun update(deltaTime: Float): Boolean {
    if (totalTime == 0f) {
      LOG.debug { "Executing effect ${effect::class.simpleName} for entity $source" }
      effect.start(this)
    }

    totalTime += deltaTime
    return if (effect.update(this, deltaTime, totalTime)) {
      effect.end(this)
      true
    } else {
      false
    }
  }

  override fun reset() {
    totalTime = 0f
    effect.reset()
  }

  companion object {
    private val LOG = logger<CombatOrder>()
  }
}
