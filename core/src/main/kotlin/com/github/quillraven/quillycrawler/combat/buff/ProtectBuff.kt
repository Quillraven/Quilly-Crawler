package com.github.quillraven.quillycrawler.combat.buff

import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.event.CombatPreDamageEvent
import com.github.quillraven.quillycrawler.event.GameEvent

class ProtectBuff(combatContext: CombatContext) : Buff(combatContext) {
  private var amount = 3
  private val reduceDamage = 0.5f

  override fun onEvent(event: GameEvent) {
    if (event is CombatPreDamageEvent && entity == event.damageEmitterComponent.target) {
      event.damageEmitterComponent.physicalDamage *= reduceDamage
      --amount
      audioService.play(SoundAssets.PROTECT)
    }
  }

  override fun isFinished(): Boolean {
    return amount <= 0
  }

  override fun reset() {
    amount = 3
  }
}
