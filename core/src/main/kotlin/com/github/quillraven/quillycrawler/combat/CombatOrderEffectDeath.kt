package com.github.quillraven.quillycrawler.combat

object CombatOrderEffectDeath : CombatOrderEffect {
  override val aiType: CombatAiType = CombatAiType.UNDEFINED

  override val manaCost: Int = 0

  override val targetType: TargetType = TargetType.NO_TARGET

  override fun start(order: CombatOrder) {
    order.playAnimation("idle")
    order.fadeTo(1f, 0f, 0f, 0f, 0.75f)
  }

  override fun update(order: CombatOrder, deltaTime: Float): Boolean {
    return order.totalTime >= 0.75f
  }

  override fun end(order: CombatOrder) {
    order.killEntity()
  }
}
