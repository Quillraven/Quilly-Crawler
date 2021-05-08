package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatOrder

object CombatOrderEffectAttack : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.OFFENSIVE

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.SINGLE_TARGET

  override fun start(order: CombatOrder) {
    order.audioService.play(SoundAssets.PUNCH_01)
    order.source.playAnimation("idle")
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.source.animationCmp.isAnimationFinished()
  }

  override fun end(order: CombatOrder) {
    order.dealAttackDamage()
  }
}
