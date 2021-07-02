package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.screen.GameScreen
import com.github.quillraven.quillycrawler.ui.SkinImages
import ktx.ashley.configureEntity
import ktx.ashley.contains
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.contains
import ktx.collections.gdxArrayOf

interface ShopListener {
  fun onSelectionChange(newIndex: Int, regionKey: String, description: String) = Unit

  fun onItemsUpdated(items: GdxArray<String>, selectionIndex: Int) = Unit
}

data class ShopViewModel(
  val bundle: I18NBundle,
  val engine: Engine,
  var playerEntity: Entity,
  val audioService: AudioService
) {
  // TODO
  //  switch SELL/BUY mode
  //  add info to bottom of UI to indicate how to switch betweeen SELL/BUY (L/R keys?)
  //  when switching mode then update UI with new items
  //  whenever buying/selling an item then open a popup to ask for confirmation like we do it in reset dungeon
  //  item sell price is 75% of its original cost
  //  attribute tomes get more expensive every time you buy them (maybe separate component that stores the amount data? or compare current stats to base stats?)
  //  ability tomes are removed/filtered if player already knows that ability
  //  maybe we need to keep the stats table in the UI to show if an item increases/decreases the stats when the player wants to buy it
  //  make items scrollable
  private var selectedItemIndex = -1
  private val itemStrings = gdxArrayOf<String>()
  private val itemEntities = gdxArrayOf<Entity>()
  private val listeners = GdxSet<ShopListener>()

  fun addShopListener(listener: ShopListener) = listeners.add(listener)

  fun removeShopListener(listener: ShopListener) = listeners.remove(listener)

  private fun GdxSet<InventoryListener>.dispatchBagUpdate() {
    this.forEach {
      it.onBagUpdated(itemStrings, selectedItemIndex)
      if (hasValidIndex()) {
        val itemCmp = itemEntities[selectedItemIndex].itemCmp
        it.onSelectionChange(selectedItemIndex, itemRegionKey(itemCmp), itemDescription(itemCmp))
      } else {
        it.onSelectionChange(selectedItemIndex, SkinImages.UNDEFINED.regionKey, "")
      }
    }
  }

  private fun itemName(itemCmp: ItemComponent) = bundle["Item.${itemCmp.itemType.name}.name"]

  private fun itemDescription(itemCmp: ItemComponent) = bundle["Item.${itemCmp.itemType.name}.description"]

  private fun itemRegionKey(itemCmp: ItemComponent) = bundle["Item.${itemCmp.itemType.name}.skinRegionKey"]

  private fun hasValidIndex() = selectedItemIndex >= 0 && selectedItemIndex < itemEntities.size

  fun load() {
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

    // listeners.dispatchBagUpdate()
    // statsAndGearInfo()
  }

  fun moveItemSelectionIndex(indicesToMove: Int) {
    audioService.play(SoundAssets.MENU_SELECT)

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
      listeners.forEach { it.onSelectionChange(selectedItemIndex, itemRegionKey(itemCmp), itemDescription(itemCmp)) }
    } else {
      listeners.forEach { it.onSelectionChange(selectedItemIndex, SkinImages.UNDEFINED.regionKey, "") }
    }
    // statsAndGearInfo()
  }

  private fun StringBuilder.appendStatusValue(value: Float) {
    if (value > 0f) {
      append("[#54CC43]+").append(value.toInt()).append("[]")
    } else {
      append("[#FF4542]").append(value.toInt()).append("[]")
    }
  }

  private fun StringBuilder.appendStatDifferenceText(statsCmp: StatsComponent, type: StatsType) {
    val baseValue = statsCmp[type]
    val totalValue = statsCmp.totalStatValue(playerEntity, type)

    if (totalValue > baseValue + 0.5f || totalValue < baseValue - 0.5f) {
      // bonus /malus stats
      appendStatusValue(totalValue - baseValue)
    }
  }

  private fun gearComparisonInfo(
    itemCmp: ItemComponent,
    type: StatsType,
    itemStatsCmp: StatsComponent,
    strBuilder: StringBuilder
  ) {
    val currentGear = playerEntity.gearCmp.gear
    val diffValue = if (itemCmp.gearType in currentGear) {
      when (type) {
        StatsType.LIFE -> currentGear[itemCmp.gearType].statsCmp[StatsType.MAX_LIFE] - itemStatsCmp[StatsType.MAX_LIFE]
        StatsType.MANA -> currentGear[itemCmp.gearType].statsCmp[StatsType.MAX_MANA] - itemStatsCmp[StatsType.MAX_MANA]
        else -> currentGear[itemCmp.gearType].statsCmp[type] - itemStatsCmp[type]
      }
    } else {
      when (type) {
        StatsType.LIFE -> itemStatsCmp[StatsType.MAX_LIFE]
        StatsType.MANA -> itemStatsCmp[StatsType.MAX_MANA]
        else -> itemStatsCmp[type]
      }
    }

    if (diffValue != 0f) {
      strBuilder.append(" [#434cFF]=>[]").appendStatusValue(diffValue)
    }
  }

  private fun consumableInfo(
    type: StatsType,
    itemStatsCmp: StatsComponent,
    strBuilder: StringBuilder
  ) {
    when (type) {
      StatsType.LIFE -> {
        if (StatsType.MAX_LIFE in itemStatsCmp.stats) {
          strBuilder.append(" [#434cFF]=>[]").appendStatusValue(itemStatsCmp[StatsType.MAX_LIFE])
        }
      }
      StatsType.MANA -> {
        if (StatsType.MAX_MANA in itemStatsCmp.stats) {
          strBuilder.append(" [#434cFF]=>[]").appendStatusValue(itemStatsCmp[StatsType.MAX_MANA])
        }
      }
      else -> {
        if (type in itemStatsCmp.stats) {
          strBuilder.append(" [#434cFF]=>[]").appendStatusValue(itemStatsCmp[type])
        }
      }
    }
  }

  private fun basicStatsInfo(
    type: StatsType,
    strBuilder: StringBuilder,
    playerStatsCmp: StatsComponent
  ) {
    when (type) {
      StatsType.LIFE -> {
        strBuilder.append(bundle["LIFE"]).append(": ")
          .append(playerStatsCmp[StatsType.LIFE].toInt())
          .append("/")
          .append(playerStatsCmp[StatsType.MAX_LIFE].toInt())
          .appendStatDifferenceText(playerStatsCmp, StatsType.MAX_LIFE)
      }
      StatsType.MANA -> {
        strBuilder.append(bundle["MANA"]).append(": ")
          .append(playerStatsCmp[StatsType.MANA].toInt())
          .append("/")
          .append(playerStatsCmp[StatsType.MAX_MANA].toInt())
          .appendStatDifferenceText(playerStatsCmp, StatsType.MAX_MANA)
      }
      else -> {
        strBuilder.append(bundle[type.name]).append(": ")
          .append(playerStatsCmp[type].toInt())
          .appendStatDifferenceText(playerStatsCmp, type)
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

  fun equipOrUseSelectedItem() {
    if (!hasValidIndex()) {
      return
    }

    audioService.play(SoundAssets.MENU_SELECT_2)

    val selectedItem = itemEntities[selectedItemIndex]
    selectedItem.itemCmp.also { itemCmp ->
      if (itemCmp.gearType != GearType.UNDEFINED) {
        // gear item -> equip it
        addGear(selectedItem)
      } else if (ConsumableComponent.MAPPER in selectedItem) {
        // consumable item -> consume it
        engine.configureEntity(playerEntity) {
          with<ConsumeComponent> {
            itemsToConsume.add(selectedItem)
          }
        }
        engine.update(0f)

        // update items if consumable got removed
        val idxOf = itemEntities.indexOf(selectedItem)
        if (selectedItem.components.size() == 0) {
          itemStrings.removeIndex(idxOf)
          itemEntities.removeIndex(idxOf)

          selectedItemIndex = when {
            // no more items left
            itemStrings.isEmpty -> -1
            // first item or item in the middle of the bag got removed -> select new item at the same index
            selectedItemIndex < itemStrings.size -> selectedItemIndex
            // last item got removed -> select last item again
            else -> itemStrings.size - 1
          }
        } else {
          // update remaining amount of item
          itemStrings[idxOf] = "${itemCmp.amount}x ${itemName(itemCmp)}"
        }
        // listeners.dispatchBagUpdate()
      }
    }

    // statsAndGearInfo()
  }

  fun returnToGame() {
    engine.run {
      configureEntity(playerEntity) {
        with<SetScreenComponent> { screenType = GameScreen::class }
      }
      update(0f)
    }
  }

  fun gold(): Int {
    return playerEntity.bagCmp.gold
  }
}
