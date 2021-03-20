package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.screen.GameScreen
import com.github.quillraven.quillycrawler.ui.SkinImages
import ktx.ashley.configureEntity
import ktx.ashley.contains
import ktx.ashley.get
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.contains
import ktx.collections.gdxArrayOf
import java.util.*

interface InventoryListener {
  fun onSelectionChange(newIndex: Int, regionKey: String, description: String) = Unit

  fun onStatsUpdated(statsInfo: EnumMap<StatsType, StringBuilder>) = Unit

  fun onGearUpdated(gearInfo: EnumMap<GearType, StringBuilder>) = Unit

  fun onBagUpdated(items: GdxArray<String>, selectionIndex: Int) = Unit
}

data class InventoryViewModel(val bundle: I18NBundle, val engine: Engine, var playerEntity: Entity) {
  private var selectedItemIndex = -1
  private val itemStrings = gdxArrayOf<String>()
  private val itemEntities = gdxArrayOf<Entity>()
  private val statsInfo = EnumMap<StatsType, StringBuilder>(StatsType::class.java)
  private val gearInfo = EnumMap<GearType, StringBuilder>(GearType::class.java)
  private val listeners = GdxSet<InventoryListener>()

  fun addInventoryListener(listener: InventoryListener) = listeners.add(listener)

  fun removeInventoryListener(listener: InventoryListener) {
    listeners.remove(listener)
  }

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

    listeners.dispatchBagUpdate()
    statsAndGearInfo()
  }

  fun moveItemSelectionIndex(indicesToMove: Int) {
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
    statsAndGearInfo()
  }

  private fun StringBuilder.appendStatusValue(value: Float) {
    if (value > 0f) {
      append(" [#54CC43]+").append(value.toInt()).append("[]")
    } else {
      append(" [#FF4542]").append(value.toInt()).append("[]")
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

  private fun statsAndGearInfo() {
    updateStatsInfo()
    updateGearInfo()
  }

  private fun updateGearInfo() {
    with(playerEntity.gearCmp.gear) {
      GearType.VALUES.forEach { type ->
        if (type == GearType.UNDEFINED) {
          return@forEach
        }

        val stringBuilder = gearInfo.getOrPut(type) { StringBuilder(20) }
        stringBuilder.clear()

        stringBuilder.append(bundle[type.name]).append(": ")

        if (type in this) {
          stringBuilder.append(itemName(this[type].itemCmp))
        } else {
          stringBuilder.append("-")
        }

        if (stringBuilder.length > 20) {
          stringBuilder.setLength(19)
          stringBuilder.append(".")
        }
      }
    }

    listeners.forEach { it.onGearUpdated(gearInfo) }
  }

  private fun updateStatsInfo() {
    val playerStatsCmp = playerEntity.statsCmp
    val selectedItem: Entity? = if (hasValidIndex()) {
      itemEntities[selectedItemIndex]
    } else {
      null
    }

    StatsType.VALUES.forEach { type ->
      if (type == StatsType.MAX_LIFE || type == StatsType.MAX_MANA) {
        return@forEach
      }

      val strBuilder = statsInfo.getOrPut(type) { StringBuilder(20) }
      strBuilder.clear()

      // build basic stat information (=base stats + current gear)
      basicStatsInfo(type, strBuilder, playerStatsCmp)

      // append stat manipulation of currently selected item
      if (selectedItem != null) {
        val itemCmp = selectedItem.itemCmp
        val itemStatsCmp = selectedItem[StatsComponent.MAPPER] ?: return@forEach

        if (itemCmp.gearType == GearType.UNDEFINED) {
          // no gear -> if it is a consumable then show how it will manipulate the stats
          consumableInfo(type, itemStatsCmp, strBuilder)
        } else {
          // gear -> compare with current gear
          gearComparisonInfo(itemCmp, type, itemStatsCmp, strBuilder)
        }
      }
    }

    listeners.forEach { it.onStatsUpdated(statsInfo) }
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
      strBuilder.append(" [#434cFF]<>[]").appendStatusValue(diffValue)
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
          strBuilder.append(" [#434cFF]<>[]").appendStatusValue(itemStatsCmp[StatsType.MAX_LIFE])
        }
      }
      StatsType.MANA -> {
        if (StatsType.MAX_MANA in itemStatsCmp.stats) {
          strBuilder.append(" [#434cFF]<>[]").appendStatusValue(itemStatsCmp[StatsType.MAX_MANA])
        }
      }
      else -> {
        if (type in itemStatsCmp.stats) {
          strBuilder.append(" [#434cFF]<>[]").appendStatusValue(itemStatsCmp[type])
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
          .append(" / ")
          .append(playerStatsCmp[StatsType.MAX_LIFE].toInt())
          .appendStatDifferenceText(playerStatsCmp, StatsType.MAX_LIFE)
      }
      StatsType.MANA -> {
        strBuilder.append(bundle["MANA"]).append(": ")
          .append(playerStatsCmp[StatsType.MANA].toInt())
          .append(" / ")
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
        if (selectedItem.components.size() == 0) {
          val idxOf = itemEntities.indexOf(selectedItem)
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

          listeners.dispatchBagUpdate()
        }
      }
    }

    statsAndGearInfo()
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
