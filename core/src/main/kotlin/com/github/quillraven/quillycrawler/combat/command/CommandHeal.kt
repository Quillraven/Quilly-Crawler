package com.github.quillraven.quillycrawler.combat.command

import com.badlogic.gdx.utils.Align
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.heal
import com.github.quillraven.quillycrawler.ashley.createEffectEntity
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandHeal(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.SUPPORTIVE

  override val manaCost: Int = 3

  override val targetType = CommandTargetType.SINGLE_TARGET

  override fun onStart() {
    entity.playAnimation("idle")
    entity.heal(engine, targets, 30f, 0f, 0f)

    audioService.play(SoundAssets.HEAL)

    targets.forEach { engine.createEffectEntity(it, "HEAL", Align.top, 1.5f) }
  }

  override fun isFinished(): Boolean = totalTime >= 1.25f
}
