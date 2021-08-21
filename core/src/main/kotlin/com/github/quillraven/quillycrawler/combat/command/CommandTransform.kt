package com.github.quillraven.quillycrawler.combat.command

import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.commons.ashley.component.renderCmp
import com.github.quillraven.commons.ashley.component.resizeTo
import com.github.quillraven.commons.ashley.component.transformCmp
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext

class CommandTransform(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.UNDEFINED
  override val manaCost: Int = 0
  override val targetType = CommandTargetType.NO_TARGET

  override fun onStart() {
    // set unit alive
    with(entity.statsCmp) {
      this[StatsType.LIFE] = this[StatsType.MAX_LIFE] * 0.75f
      this[StatsType.MANA] = this[StatsType.MAX_MANA]
    }

    val renderCmp = entity.renderCmp

    // change new graphic to greener color and bigger size
    entity.fadeTo(
      engine,
      (renderCmp.sprite.color.r - 0.75f).coerceAtLeast(0f),
      renderCmp.sprite.color.g,
      (renderCmp.sprite.color.b - 0.5f).coerceAtLeast(0f),
      renderCmp.sprite.color.a,
      4f
    )

    val size = entity.transformCmp.size
    entity.resizeTo(engine, size.x * 1.25f, size.y * 1.25f, 4f)

    // play transform sound
    audioService.play(SoundAssets.TRANSFORM_DEMON)
  }

  override fun isFinished() = totalTime >= 4.5f

}
