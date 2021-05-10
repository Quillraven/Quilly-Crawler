package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandDeath(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.UNDEFINED

  override val manaCost: Int = 0

  override val targetType = CommandTargetType.NO_TARGET

  override fun onStart() {
    entity.playAnimation("idle")
    entity.fadeTo(engine, 1f, 0f, 0f, 0f, 0.75f)
  }

  override fun isFinished(): Boolean {
    return totalTime >= 0.75f
  }
}
