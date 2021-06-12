package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.dealAttackDamage
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandAttack(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.OFFENSIVE
  override val manaCost: Int = 0
  override val targetType = CommandTargetType.SINGLE_TARGET
  private var executeAttack = true

  override fun onStart() {
    entity.playAnimation("idle")
  }

  override fun onUpdate(deltaTime: Float) {
    if (executeAttack && totalTime >= 0.25f) {
      executeAttack = false
      audioService.play(SoundAssets.PUNCH_01)
      entity.dealAttackDamage(engine, targets)
    }
  }

  override fun reset() {
    super.reset()
    executeAttack = true
  }
}
