package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.dealDamage
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandExplosion(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.OFFENSIVE
  override val manaCost: Int = 10
  override val targetType = CommandTargetType.ALL_TARGETS

  override fun onStart() {
    entity.playAnimation("idle")
    entity.dealDamage(engine, targets, 0f, 100f, 0.25f)
  }
}
