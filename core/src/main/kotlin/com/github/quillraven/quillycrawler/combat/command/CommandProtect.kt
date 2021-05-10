package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.addBuff
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.buff.ProtectBuff

class CommandProtect(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.DEFENSIVE

  override val manaCost: Int = 5

  override val targetType = CommandTargetType.NO_TARGET

  override fun onStart() {
    entity.playAnimation("idle")
  }

  override fun isFinished(): Boolean {
    return entity.animationCmp.isAnimationFinished()
  }

  override fun onFinish() {
    entity.addBuff(ProtectBuff::class)
  }
}
