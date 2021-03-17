package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.screen.GameScreen
import com.github.quillraven.quillycrawler.ui.SkinImages
import ktx.ashley.configureEntity
import ktx.ashley.with
import ktx.collections.gdxArrayOf

data class InventoryViewModel(val bundle: I18NBundle, val engine: Engine, var playerEntity: Entity) {
  private var selectedItemIndex = -1
  private val itemStrings = gdxArrayOf<String>()
  private val itemEntities = gdxArrayOf<Entity>()

  fun initialize() {
    selectedItemIndex = -1
    itemStrings.clear()
    itemEntities.clear()
    playerEntity.bagCmp.items.values().forEach { item ->
      itemEntities.add(item)
      item.itemCmp.also { itemCmp ->
        itemStrings.add("${itemCmp.amount}x ${bundle["Item.${itemCmp.itemType.name}.name"]}")
      }
    }
  }

  private fun addGear(itemEntity: Entity) {
    engine.run {
      configureEntity(playerEntity) {
        with<EquipComponent> {
          addToGear.add(itemEntity)
        }
      }
      update(0f)
    }
  }

  fun items() = itemStrings

  private fun selectedItem(): Entity? {
    with(playerEntity.bagCmp) {
      return if (selectedItemIndex == -1 || selectedItemIndex >= items.size) {
        // invalid index
        null
      } else {
        itemEntities[selectedItemIndex]
      }
    }
  }

  fun selectedItemRegionKey(): String {
    val selectedItem = selectedItem()
    return if (selectedItem == null) {
      SkinImages.UNDEFINED.regionKey
    } else {
      bundle["Item.${selectedItem.itemCmp.itemType.name}.skinRegionKey"]
    }
  }

  fun selectedItemDescription(): String {
    val selectedItem = selectedItem()
    return if (selectedItem == null) {
      ""
    } else {
      bundle["Item.${selectedItem.itemCmp.itemType.name}.description"]
    }
  }

  fun selectFirstItem(): Int {
    selectedItemIndex = if (playerEntity.bagCmp.items.isEmpty) -1 else 0
    return selectedItemIndex
  }

  fun moveItemSelectionIndex(indicesToMove: Int): Int {
    with(playerEntity.bagCmp) {
      if (items.isEmpty) {
        // entity has no items -> do nothing
        selectedItemIndex = -1
      } else {
        // move index and check for overflow/underflow
        selectedItemIndex += indicesToMove
        if (selectedItemIndex >= items.size) {
          selectedItemIndex = 0
        } else if (selectedItemIndex < 0) {
          selectedItemIndex = items.size - 1
        }
      }
    }

    return selectedItemIndex
  }

  fun equipOrUseSelectedItem() {
    val selectedItem = selectedItem()
    if (selectedItem == null) {
      return
    } else {
      selectedItem.itemCmp.also { itemCmp ->
        if (itemCmp.gearType != GearType.UNDEFINED) {
          addGear(itemEntities[selectedItemIndex])
        }

        // TODO use item if it is a consumable
      }
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
