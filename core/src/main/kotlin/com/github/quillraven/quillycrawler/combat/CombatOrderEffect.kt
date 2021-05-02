package com.github.quillraven.quillycrawler.combat

interface CombatOrderEffect {
  fun start(order: CombatOrder) = Unit

  fun update(order: CombatOrder, deltaTime: Float, totalTime: Float): Boolean = true

  fun end(order: CombatOrder) = Unit

  fun reset() = Unit
}

object CombatOrderEffectUndefined : CombatOrderEffect
