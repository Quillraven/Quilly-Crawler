package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandDeath(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.UNDEFINED
  override val manaCost: Int = 0
  override val targetType = CommandTargetType.NO_TARGET
  var targetAlpha = 0f

  override fun onStart() {
    entity.fadeTo(engine, 1f, 0f, 0f, targetAlpha, 0.75f)
    audioService.play(SoundAssets.DEATH)
  }

  override fun isFinished(): Boolean {
    return totalTime >= 0.75f
  }

  override fun onFinish() {
    entity.combatCmp.defeated = true
  }

  override fun reset() {
    super.reset()
    targetAlpha = 0f
  }
}
