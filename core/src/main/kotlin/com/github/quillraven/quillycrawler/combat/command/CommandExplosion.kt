package com.github.quillraven.quillycrawler.combat.command

import com.badlogic.gdx.utils.Align
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.quillycrawler.ashley.component.dealDamage
import com.github.quillraven.quillycrawler.ashley.createEffectEntity
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandExplosion(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.OFFENSIVE
  override val manaCost: Int = 10
  override val targetType = CommandTargetType.ALL_TARGETS

  override fun onStart() {
    entity.playAnimation("idle")
    entity.dealDamage(engine, targets, 0f, 100f, 0.75f)
    audioService.play(SoundAssets.EXPLOSION)
    targets.forEach {
      engine.createEffectEntity(it, "FIRE_RING", Align.center, 1.4f, speed = 2f, scaling = 1.5f)
    }
  }

  override fun isFinished(): Boolean = totalTime >= 1.2f
}
