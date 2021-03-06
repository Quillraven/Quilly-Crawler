package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.withinRange
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.ashley.component.ActionableComponent
import com.github.quillraven.quillycrawler.ashley.component.InteractComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerComponent
import com.github.quillraven.quillycrawler.ashley.component.interactCmp
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class MapSystem(
  private val mapService: MapService,
  private var currentLevel: Int = 0
) : IteratingSystem(
  allOf(PlayerComponent::class, InteractComponent::class).exclude(RemoveComponent::class).get()
) {
  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    mapService.setMap(engine, "maps/tutorial.tmx")
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val entitiesInRange = entity.interactCmp.entitiesInRange
    if (entitiesInRange.isEmpty) {
      return
    }

    // check if there is an exit entity within range of the player to move to the next dungeon level
    entitiesInRange.forEach { otherEntity ->
      otherEntity[ActionableComponent.MAPPER]?.let { actionableCmp ->
        if (actionableCmp.isExit && entity.withinRange(otherEntity)) {
          moveToNextLevel()
        }
      }
    }
  }

  private fun moveToNextLevel() {
    currentLevel++
    LOG.debug { "Moving to dungeon level $currentLevel" }

    val nextMapFilePath = nextMap(currentLevel)
    if (nextMapFilePath.isNotBlank()) {
      mapService.setMap(engine, nextMapFilePath)
    } else {
      --currentLevel
      LOG.debug { "You reached the end of the dungeon!" }
    }
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
