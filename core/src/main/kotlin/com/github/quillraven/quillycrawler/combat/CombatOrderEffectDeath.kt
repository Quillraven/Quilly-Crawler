package com.github.quillraven.quillycrawler.combat

import com.github.quillraven.commons.ashley.component.animationCmp

object CombatOrderEffectDeath : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.UNDEFINED

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.NO_TARGET

  override fun start(order: CombatOrder) {
    order.playAnimation("idle")
    // TODO fadeout and change color to red --> create new component and system for that
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.source.animationCmp.isAnimationFinished()
  }

  override fun end(order: CombatOrder) {
    order.killEntity()
  }
}
