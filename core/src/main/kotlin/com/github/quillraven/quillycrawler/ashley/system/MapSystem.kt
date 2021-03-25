package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.ashley.component.GoToNextLevelComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerComponent
import com.github.quillraven.quillycrawler.ashley.component.playerCmp
import com.github.quillraven.quillycrawler.assets.MusicAssets
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class MapSystem(
  private val mapService: MapService,
  private val audioService: AudioService
) : IteratingSystem(
  allOf(PlayerComponent::class, GoToNextLevelComponent::class).exclude(RemoveComponent::class).get()
) {
  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    mapService.setMap(engine, "maps/tutorial.tmx")
    audioService.playMusic(MusicAssets.TRY_AND_SOLVE_THIS.descriptor.fileName)
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val playerCmp = entity.playerCmp
    playerCmp.dungeonLevel++
    LOG.debug { "Moving to dungeon level ${playerCmp.dungeonLevel}" }

    // TODO remove debug stuff -> when shall we change the music? every 5th level for a boss fight?
    if (playerCmp.dungeonLevel == 2) {
      audioService.playMusic(MusicAssets.TAKE_COVER.descriptor.fileName)
    }

    val nextMapFilePath = nextMap(playerCmp.dungeonLevel)
    if (nextMapFilePath.isNotBlank()) {
      mapService.setMap(engine, nextMapFilePath)
    } else {
      --playerCmp.dungeonLevel
      LOG.debug { "You reached the end of the dungeon!" }
    }

    entity.remove(GoToNextLevelComponent::class.java)
  }

  private fun nextMap(dungeonLevel: Int): String {
    val folderPath = "maps/level_${dungeonLevel}"
    val mapFolder = Gdx.files.internal(folderPath)
    if (!mapFolder.exists()) {
      LOG.debug { "Map folder '$folderPath' does not exist" }
      return ""
    }

    val mapFiles = mapFolder.list(".tmx")
    if (mapFiles.isEmpty()) {
      LOG.error { "Map folder '$folderPath' has no .tmx files" }
      return ""
    }

    return mapFiles.random().path()
  }

  companion object {
    private val LOG = logger<MapSystem>()
  }
}
