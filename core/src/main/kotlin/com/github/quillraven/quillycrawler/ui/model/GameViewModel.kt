package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.GoToLevel
import com.github.quillraven.quillycrawler.ashley.component.bagCmp
import com.github.quillraven.quillycrawler.ashley.component.playerCmp
import com.github.quillraven.quillycrawler.ashley.system.PlayerControlSystem
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.event.GameEvent
import com.github.quillraven.quillycrawler.event.GameEventListener
import com.github.quillraven.quillycrawler.event.GameInteractReaperEvent
import com.github.quillraven.quillycrawler.event.MapChangeEvent
import ktx.ashley.configureEntity
import ktx.ashley.getSystem
import ktx.ashley.with
import ktx.collections.GdxSet

interface GameUiListener {
  fun onMapChange(mapName: StringBuilder) = Unit

  fun onDungeonReset(goldLoss: Int, newLevel: Int) = Unit
}

data class GameViewModel(val bundle: I18NBundle, val engine: Engine, private val audioService: AudioService) :
  GameEventListener {
  private val uiListeners = GdxSet<GameUiListener>()
  private val mapNameBuilder = StringBuilder()
  private lateinit var playerEntity: Entity

  fun addGameListener(listener: GameUiListener) = uiListeners.add(listener)

  fun removeGameListener(listener: GameUiListener) = uiListeners.remove(listener)

  fun resetDungeon(reset: Boolean) {
    engine.getSystem<PlayerControlSystem>().setProcessing(true)

    if (reset) {
      val bagCmp = playerEntity.bagCmp
      val goldToPay = (bagCmp.gold * 0.05f).toInt()
      bagCmp.gold -= goldToPay
      val targetDungeonLvl = (playerEntity.playerCmp.dungeonLevel - 5).coerceAtLeast(1)
      engine.configureEntity(playerEntity) {
        with<GoToLevel> {
          targetLevel = targetDungeonLvl
        }
      }
      audioService.play(SoundAssets.POWER_UP_12)
    } else {
      audioService.play(SoundAssets.MENU_BACK)
    }
  }

  fun switchSelection() {
    audioService.play(SoundAssets.MENU_SELECT)
  }

  override fun onEvent(event: GameEvent) {
    if (event is MapChangeEvent) {
      mapNameBuilder.clear()
      mapNameBuilder.append(bundle["GameView.dungeonLevel"]).append(" ").append(event.level)
      uiListeners.forEach { it.onMapChange(mapNameBuilder) }
    } else if (event is GameInteractReaperEvent) {
      playerEntity = event.entity
      val goldToPay = (playerEntity.bagCmp.gold * 0.05f).toInt()
      val targetDungeonLvl = (playerEntity.playerCmp.dungeonLevel - 5).coerceAtLeast(1)

      engine.getSystem<PlayerControlSystem>().setProcessing(false)
      uiListeners.forEach { it.onDungeonReset(goldToPay, targetDungeonLvl) }
    }
  }
}
