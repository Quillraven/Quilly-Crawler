package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.heal
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandHeal(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.SUPPORTIVE

  override val manaCost: Int = 3

  override val targetType = CommandTargetType.SINGLE_TARGET

  override fun onStart() {
    entity.playAnimation("idle")
    entity.heal(engine, targets, 30f, 0f, 0f)
  }
}
