package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.GoToLevel
import com.github.quillraven.quillycrawler.ashley.component.bagCmp
import com.github.quillraven.quillycrawler.ashley.component.playerCmp
import com.github.quillraven.quillycrawler.ashley.system.PlayerControlSystem
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.configureEntity
import ktx.ashley.getSystem
import ktx.ashley.with
import ktx.collections.GdxSet
import ktx.collections.isNotEmpty

interface GameUiListener {
  fun onMapChange(mapName: StringBuilder) = Unit

  fun onDungeonReset(goldLoss: Int, newLevel: Int) = Unit

  fun onGameExit() = Unit

  fun onPlayerLoot(lootDescr: StringBuilder) = Unit
}

data class GameViewModel(val bundle: I18NBundle, val engine: Engine, private val audioService: AudioService) :
  GameEventListener {
  private val uiListeners = GdxSet<GameUiListener>()
  private val strBuilder = StringBuilder()
  private lateinit var playerEntity: Entity

  fun addGameListener(listener: GameUiListener) = uiListeners.add(listener)

  fun removeGameListener(listener: GameUiListener) = uiListeners.remove(listener)

  fun exitGame(exit: Boolean) {
    engine.getSystem<PlayerControlSystem>().setProcessing(true)

    if (exit) {
      Gdx.app.exit()
    } else {
      audioService.play(SoundAssets.MENU_BACK)
    }
  }

  fun backToGame() {
    audioService.play(SoundAssets.MENU_SELECT)
    engine.getSystem<PlayerControlSystem>().setProcessing(true)
  }

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
    when (event) {
      is MapChangeEvent -> {
        strBuilder.clear()
        strBuilder.append(bundle["GameView.dungeonLevel"]).append(" ").append(event.level)
        uiListeners.forEach { it.onMapChange(strBuilder) }
      }
      is GameInteractReaperEvent -> {
        playerEntity = event.entity
        val goldToPay = (playerEntity.bagCmp.gold * 0.05f).toInt()
        val targetDungeonLvl = (playerEntity.playerCmp.dungeonLevel - 5).coerceAtLeast(1)

        engine.getSystem<PlayerControlSystem>().setProcessing(false)
        uiListeners.forEach { it.onDungeonReset(goldToPay, targetDungeonLvl) }
      }
      is GameExitEvent -> {
        engine.getSystem<PlayerControlSystem>().setProcessing(false)
        uiListeners.forEach { it.onGameExit() }
      }
      is GameLootEvent -> {
        strBuilder.clear()
        if (event.items.isNotEmpty()) {
          strBuilder.append(bundle.format("GameView.loot-with-items", event.gold))
          strBuilder.append("\n")
          event.items.forEach { strBuilder.append("\n").append(bundle["Item.${it.name}.name"]) }
        } else {
          strBuilder.append(bundle.format("GameView.loot-no-items", event.gold))
        }
        engine.getSystem<PlayerControlSystem>().setProcessing(false)
        uiListeners.forEach { it.onPlayerLoot(strBuilder) }
      }
      else -> Unit
    }
  }
}
