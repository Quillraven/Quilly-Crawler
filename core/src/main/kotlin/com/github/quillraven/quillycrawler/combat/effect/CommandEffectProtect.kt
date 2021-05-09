package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.addBuff
import com.github.quillraven.quillycrawler.combat.Command
import com.github.quillraven.quillycrawler.combat.buff.ProtectBuff

object CommandEffectProtect : CommandEffect {
  override val aiType: CombatAiType = CombatAiType.DEFENSIVE

  override val manaCost: Int = 5

  override val targetType: TargetType = TargetType.NO_TARGET

  override fun start(order: Command) {
    order.source.playAnimation("idle")
  }

  override fun update(order: Command, deltaTime: Float): Boolean {
    return order.source.animationCmp.isAnimationFinished()
  }

  override fun end(order: Command) {
    order.source.addBuff(ProtectBuff::class)
  }
}
