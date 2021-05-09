package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.Command

object CommandEffectAttack : CommandEffect {
  override val aiType: CombatAiType = CombatAiType.OFFENSIVE

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.SINGLE_TARGET

  override fun start(order: Command) {
    order.audioService.play(SoundAssets.PUNCH_01)
    order.source.playAnimation("idle")
  }

  override fun update(order: Command, deltaTime: Float): Boolean {
    return order.source.animationCmp.isAnimationFinished()
  }

  override fun end(order: Command) {
    order.dealAttackDamage()
  }
}
