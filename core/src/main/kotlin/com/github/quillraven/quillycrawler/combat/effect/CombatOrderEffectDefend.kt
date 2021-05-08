package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatOrder

object CombatOrderEffectDefend : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.DEFENSIVE

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.NO_TARGET

  override fun start(order: CombatOrder) {
    order.audioService.play(SoundAssets.DEFEND_01)
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.totalTime >= 0.5f
  }
}
