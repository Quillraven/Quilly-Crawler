package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.dealAttackDamage
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandAttack(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.OFFENSIVE

  override val manaCost: Int = 0

  override val targetType = CommandTargetType.SINGLE_TARGET

  override fun onStart() {
    audioService.play(SoundAssets.PUNCH_01)
    entity.playAnimation("idle")
  }

  override fun isFinished(): Boolean {
    return entity.animationCmp.isAnimationFinished()
  }

  override fun onFinish() {
    entity.dealAttackDamage(engine, targets)
  }
}
