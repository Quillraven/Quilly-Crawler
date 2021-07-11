package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.ashley.component.GoToLevel
import com.github.quillraven.quillycrawler.ashley.component.PlayerComponent
import com.github.quillraven.quillycrawler.ashley.component.goToLevelCmp
import com.github.quillraven.quillycrawler.ashley.component.playerCmp
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.MapChangeEvent
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class MapSystem(
  private val mapService: MapService,
  private val gameEventDispatcher: GameEventDispatcher
) : IteratingSystem(
  allOf(PlayerComponent::class, GoToLevel::class).exclude(RemoveComponent::class).get()
) {
  private var currentMapFolder: FileHandle = FileHandle("")

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    mapService.setMap(engine, "maps/tutorial.tmx")
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val playerCmp = entity.playerCmp
    val goToLevelCmp = entity.goToLevelCmp
    if (goToLevelCmp.targetLevel < playerCmp.dungeonLevel) {
      // player used REAPER to go back in levels -> update currentMapFolder
      var lvl = goToLevelCmp.targetLevel
      var folderPath = "maps/level_${lvl}"
      var mapFolder = Gdx.files.internal(folderPath)
      while (!mapFolder.exists()) {
        --lvl
        folderPath = "maps/level_${lvl}"
        mapFolder = Gdx.files.internal(folderPath)
        if (lvl <= 0) {
          throw GdxRuntimeException("There are maps defined for level ${goToLevelCmp.targetLevel} and above")
        }
      }
      currentMapFolder = mapFolder
    }
    playerCmp.dungeonLevel = goToLevelCmp.targetLevel
    LOG.debug { "Moving to dungeon level ${playerCmp.dungeonLevel}" }

    val nextMapFilePath = nextMap(playerCmp.dungeonLevel)
    if (nextMapFilePath.isNotBlank()) {
      mapService.setMap(engine, nextMapFilePath)
    }
    gameEventDispatcher.dispatchEvent<MapChangeEvent> {
      this.entity = entity
      this.level = playerCmp.dungeonLevel
    }

    entity.remove(GoToLevel::class.java)
  }

  private fun nextMap(dungeonLevel: Int): String {
    val folderPath = "maps/level_${dungeonLevel}"
    val mapFolder = Gdx.files.internal(folderPath)
    if (!mapFolder.exists()) {
      LOG.debug { "Map folder '$folderPath' does not exist. Using ${currentMapFolder.path()} instead" }
      if (currentMapFolder.path().isBlank()) {
        return ""
      }
    } else {
      LOG.debug { "Switch map folder to '${mapFolder.path()}'" }
      currentMapFolder = mapFolder
    }

    val mapFiles = currentMapFolder.list(".tmx")
    if (mapFiles.isEmpty()) {
      LOG.error { "Map folder '${currentMapFolder.path()}' has no .tmx files" }
      return ""
    }

    return mapFiles.random().path()
  }

  companion object {
    private val LOG = logger<MapSystem>()
  }
}
