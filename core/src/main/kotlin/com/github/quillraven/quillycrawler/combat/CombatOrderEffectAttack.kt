package com.github.quillraven.quillycrawler.combat

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.quillycrawler.assets.SoundAssets

object CombatOrderEffectAttack : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.OFFENSIVE

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.SINGLE_TARGET

  override fun start(order: CombatOrder) {
    order.playSound(SoundAssets.PUNCH_01)
    order.playAnimation("idle")
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.source.animationCmp.isAnimationFinished()
  }

  override fun end(order: CombatOrder) {
    order.dealAttackDamage()
  }
}
