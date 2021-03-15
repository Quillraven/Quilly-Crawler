package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.github.quillraven.quillycrawler.ashley.component.EquipComponent
import com.github.quillraven.quillycrawler.ashley.component.SetScreenComponent
import com.github.quillraven.quillycrawler.screen.GameScreen
import ktx.ashley.configureEntity
import ktx.ashley.with

data class InventoryViewModel(val engine: Engine, var playerEntity: Entity) {
  fun addGear(itemEntity: Entity) {
    engine.run {
      configureEntity(playerEntity) {
        with<EquipComponent> {
          addToGear.add(itemEntity)
        }
      }
      update(0f)
    }
  }

  fun removeGear(itemEntity: Entity) {
    engine.run {
      configureEntity(playerEntity) {
        with<EquipComponent> {
          removeFromGear.add(itemEntity)
        }
      }
      update(0f)
    }
  }

  fun returnToGame() {
    engine.run {
      configureEntity(playerEntity) {
        with<SetScreenComponent> { screenType = GameScreen::class }
      }
      update(0f)
    }
  }
}
