package com.github.quillraven.quillycrawler.combat.effect

import com.github.quillraven.quillycrawler.combat.Command

enum class CombatAiType {
  UNDEFINED, OFFENSIVE, DEFENSIVE, SUPPORTIVE
}

enum class TargetType {
  UNDEFINED, NO_TARGET, SINGLE_TARGET
}

interface CommandEffect {
  val aiType: CombatAiType

  val manaCost: Int

  val targetType: TargetType

  fun start(order: Command) = Unit

  fun update(order: Command, deltaTime: Float): Boolean = true

  fun end(order: Command) = Unit

  fun reset() = Unit
}

object CommandEffectUndefined : CommandEffect {
  override val aiType: CombatAiType = CombatAiType.UNDEFINED
  override val manaCost: Int = 0
  override val targetType: TargetType = TargetType.UNDEFINED
}
