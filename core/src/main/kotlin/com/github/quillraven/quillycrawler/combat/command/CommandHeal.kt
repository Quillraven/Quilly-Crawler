package com.github.quillraven.quillycrawler.combat.command

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.component.heal
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext
import ktx.ashley.entity
import ktx.ashley.with

class CommandHeal(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.SUPPORTIVE

  override val manaCost: Int = 0//3

  override val targetType = CommandTargetType.SINGLE_TARGET

  override fun onStart() {
    entity.playAnimation("idle")
    entity.heal(engine, targets, 30f, 0f, 0f)

    audioService.play(SoundAssets.HEAL)

    targets.forEach { target ->
      engine.entity {
        // TODO create a "SfxComponent" and "SfxSystem" that does the calculation
        // for the correct positioning of an effect. Needs to run AFTER RenderSystem to
        // have the sprite data available
        with<AnimationComponent> {
          atlasFilePath = TextureAtlasAssets.EFFECTS.descriptor.fileName
          regionKey = "HEAL"
          stateKey = "frame"
          playMode = Animation.PlayMode.NORMAL
        }
        with<RenderComponent>()
        with<TransformComponent> {
          // 1 / sprite w/h
          val scaleToBaseY = (1 / 2f) * target.renderCmp.sprite.height * target.renderCmp.sprite.scaleY
          position.set(target.transformCmp.position).run {
            z = -1f
          }
          size.set(scaleToBaseY, scaleToBaseY)
        }
        with<RemoveComponent> {
          delay = 1.75f
        }
      }
    }
  }

  override fun isFinished(): Boolean = totalTime >= 1.5f
}
