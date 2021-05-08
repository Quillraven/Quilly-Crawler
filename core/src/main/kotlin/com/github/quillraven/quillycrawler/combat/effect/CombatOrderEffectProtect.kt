package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.combat.CombatOrder
import com.github.quillraven.quillycrawler.combat.buff.ProtectBuff

object CombatOrderEffectProtect : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.DEFENSIVE

  override val manaCost: Int = 5

  override val targetType: TargetType = TargetType.NO_TARGET

  override fun start(order: CombatOrder) {
    order.source.playAnimation("idle")
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.source.animationCmp.isAnimationFinished()
  }

  override fun end(order: CombatOrder) {
    order.addBuff(ProtectBuff::class)
  }
}
