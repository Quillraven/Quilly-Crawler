package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

enum class ItemType(val gearType: GearType = GearType.UNDEFINED) {
  UNDEFINED,
  HAT(GearType.HELMET),
  ROBE(GearType.ARMOR),
  CURSED_NECKLACE(GearType.AMULET),
  ROD(GearType.WEAPON),
  LEATHER_GLOVES(GearType.GLOVES),
  LEATHER_BOOTS(GearType.BOOTS),
  BUCKLER(GearType.SHIELD),
  HEALTH_POTION,
  MANA_POTION;

  companion object {
    private val GEAR_ITEM_TYPES = values().filter { it.gearType != GearType.UNDEFINED }

    fun randomGearItem() = GEAR_ITEM_TYPES.random()
  }
}

class ItemComponent : Component, Pool.Poolable {
  var itemType = ItemType.UNDEFINED
  var gearType = GearType.UNDEFINED
  var amount = 1
  var cost = 0

  fun name(bundle: I18NBundle): String = bundle["Item.${this.itemType.name}.name"]

  fun description(bundle: I18NBundle): String = bundle["Item.${this.itemType.name}.description"]

  fun regionKey(bundle: I18NBundle): String {
    val regionKey = bundle["Item.${this.itemType.name}.skinRegionKey"]
    if (regionKey.startsWith("???")) {
      // regionKey not defined in bundle -> return undefined region which represents a fallback region in the atlas
      return "undefined"
    }
    return regionKey
  }

  override fun reset() {
    gearType = GearType.UNDEFINED
    itemType = ItemType.UNDEFINED
    amount = 1
    cost = 0
  }

  companion object {
    val MAPPER = mapperFor<ItemComponent>()
  }
}

val Entity.itemCmp: ItemComponent
  get() = this[ItemComponent.MAPPER]
    ?: throw GdxRuntimeException("ItemComponent for entity '$this' is null")
