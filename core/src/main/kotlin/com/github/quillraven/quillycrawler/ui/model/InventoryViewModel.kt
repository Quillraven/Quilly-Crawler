package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.screen.GameScreen
import com.github.quillraven.quillycrawler.ui.SkinImages
import ktx.ashley.configureEntity
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf

data class InventoryViewModel(val bundle: I18NBundle, val engine: Engine, var playerEntity: Entity) {
  private var selectedItemIndex = -1
  private val itemStrings = gdxArrayOf<String>()
  private val itemEntities = gdxArrayOf<Entity>()

  private fun itemName(itemCmp: ItemComponent) = bundle["Item.${itemCmp.itemType.name}.name"]

  private fun itemDescription(itemCmp: ItemComponent) = bundle["Item.${itemCmp.itemType.name}.description"]

  private fun itemRegionKey(itemCmp: ItemComponent) = bundle["Item.${itemCmp.itemType.name}.skinRegionKey"]

  private fun hasValidIndex() = selectedItemIndex >= 0 && selectedItemIndex < itemEntities.size

  fun load(callback: (GdxArray<String>, Int, String, String) -> Unit) {
    itemStrings.clear()
    itemEntities.clear()
    with(playerEntity.bagCmp) {
      items.values().forEach { item ->
        itemEntities.add(item)
        item.itemCmp.also { itemCmp ->
          itemStrings.add("${itemCmp.amount}x ${itemName(itemCmp)}")
        }
      }

      selectedItemIndex = if (items.isEmpty) -1 else 0
    }

    if (hasValidIndex()) {
      val itemCmp = itemEntities[selectedItemIndex].itemCmp
      callback(itemStrings, selectedItemIndex, itemRegionKey(itemCmp), itemDescription(itemCmp))
    } else {
      callback(itemStrings, selectedItemIndex, SkinImages.UNDEFINED.regionKey, "")
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

  fun moveItemSelectionIndex(indicesToMove: Int, callback: (Int, String, String) -> Unit) {
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

    if (hasValidIndex()) {
      val itemCmp = itemEntities[selectedItemIndex].itemCmp
      callback(selectedItemIndex, itemRegionKey(itemCmp), itemDescription(itemCmp))
    } else {
      callback(selectedItemIndex, SkinImages.UNDEFINED.regionKey, "")
    }
  }

  fun equipOrUseSelectedItem() {
    if (hasValidIndex()) {
      itemEntities[selectedItemIndex].itemCmp.also { itemCmp ->
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
