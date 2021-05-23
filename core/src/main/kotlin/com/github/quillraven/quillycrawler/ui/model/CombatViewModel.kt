package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Scaling
import com.github.quillraven.commons.ashley.component.renderCmp
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.command.CommandAttack
import com.github.quillraven.quillycrawler.event.*
import com.github.quillraven.quillycrawler.screen.GameScreen
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.collections.GdxArray
import ktx.collections.GdxSet

interface CombatUiListener {
  fun onNextTurn(turn: Int, entityImages: GdxArray<Image>)
  fun onVictory() = Unit
  fun onDefeat() = Unit
}

enum class CombatState {
  RUNNING, DEFEAT, VICTORY
}

private class ImagePool : Pool<Image>() {
  override fun newObject(): Image = Image(TextureRegionDrawable(), Scaling.fit)
}

data class CombatViewModel(
  val bundle: I18NBundle,
  val engine: Engine,
  val game: QuillyCrawler,
  val audioService: AudioService = game.audioService
) : GameEventListener {
  private val listeners = GdxSet<CombatUiListener>()
  var combatState: CombatState = CombatState.RUNNING
  private val enemyEntities =
    engine.getEntitiesFor(allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get())
  private val playerEntities = engine.getEntitiesFor(
    allOf(CombatComponent::class, StatsComponent::class, PlayerComponent::class).exclude(
      CombatAIComponent::class
    ).get()
  )
  private var selectedTarget: Entity? = null
  private val imgPool = ImagePool()
  private val turnEntityImgs = GdxArray<Image>()

  fun addCombatListener(listener: CombatUiListener) = listeners.add(listener)

  fun removeCombatListener(listener: CombatUiListener) = listeners.remove(listener)

  fun selectTarget() {
    enemyEntities.forEach { entity ->
      if (entity.isAlive) {
        selectedTarget = entity
        return@forEach
      }
    }
  }

  fun orderAttack() {
    playerEntities.forEach { it.combatCmp.newCommand<CommandAttack>(selectedTarget) }
  }

  fun returnToGame() {
    game.setScreen<GameScreen>()
  }

  override fun onEvent(event: GameEvent) {
    when (event) {
      is CombatNewTurnEvent -> {
        turnEntityImgs.forEach { imgPool.free(it) }
        turnEntityImgs.clear()
        event.turnEntities.forEach { entity ->
          val sprite = entity.renderCmp.sprite
          turnEntityImgs.add(imgPool.obtain().apply {
            (drawable as TextureRegionDrawable).region = sprite
            color.set(sprite.color)
          })
        }

        listeners.forEach { it.onNextTurn(event.turn, turnEntityImgs) }
      }
      is CombatVictoryEvent -> {
        combatState = CombatState.VICTORY
        audioService.play(MusicAssets.VICTORY, loop = false)
      }
      is CombatDefeatEvent -> {
        combatState = CombatState.DEFEAT
        audioService.play(MusicAssets.DEFEAT, loop = false)
      }
      else -> Unit
    }
  }
}
