package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.combat.CombatOrder

object CombatOrderEffectDeath : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.UNDEFINED

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.NO_TARGET

  override fun start(order: CombatOrder) {
    order.source.playAnimation("idle")
    order.source.fadeTo(order.engine, 1f, 0f, 0f, 0f, 0.75f)
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.totalTime >= 0.75f
  }

  override fun end(order: CombatOrder) {
    order.source.removeFromEngine(order.engine)
  }
}
