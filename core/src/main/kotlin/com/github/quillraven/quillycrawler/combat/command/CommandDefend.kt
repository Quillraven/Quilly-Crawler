package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandDefend(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.DEFENSIVE

  override val manaCost: Int = 0

  override val targetType = CommandTargetType.NO_TARGET

  override fun onStart() {
    audioService.play(SoundAssets.DEFEND_01)
  }

  override fun isFinished(): Boolean {
    return totalTime >= 0.5f
  }
}
