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
  MANA_POTION,
  TOME_PROTECT,
  TOME_FIREBOLT,
  TOME_EXPLOSION,
  TOME_HEAL,
  TOME_STRENGTH,
  TOME_AGILITY,
  TOME_INTELLIGENCE;

  companion object {
    private val GEAR_ITEM_TYPES = values().filter { it.gearType != GearType.UNDEFINED }

    fun randomGearItem() = GEAR_ITEM_TYPES.random()
  }
}

class ItemComponent : Component, Pool.Poolable {
  var itemType = ItemType.UNDEFINED
  var gearType = GearType.UNDEFINED
  var amount = 1
  var baseCost: Int = 0

  fun cost(entity: Entity): Int {
    return when (itemType) {
      ItemType.TOME_STRENGTH -> {
        val currentStr = entity[StatsComponent.MAPPER]?.get(StatsType.STRENGTH) ?: 0f
        val strDiff = if (currentStr > StatsComponent.BASE_STRENGTH) currentStr - StatsComponent.BASE_STRENGTH else 0f
        baseCost + 100 * strDiff.toInt()
      }
      ItemType.TOME_AGILITY -> {
        val currentAgi = entity[StatsComponent.MAPPER]?.get(StatsType.AGILITY) ?: 0f
        val agiDiff = if (currentAgi > StatsComponent.BASE_AGILITY) currentAgi - StatsComponent.BASE_AGILITY else 0f
        baseCost + 100 * agiDiff.toInt()
      }
      ItemType.TOME_INTELLIGENCE -> {
        val crnInt = entity[StatsComponent.MAPPER]?.get(StatsType.INTELLIGENCE) ?: 0f
        val intDiff = if (crnInt > StatsComponent.BASE_INTELLIGENCE) crnInt - StatsComponent.BASE_INTELLIGENCE else 0f
        baseCost + 100 * intDiff.toInt()
      }
      else -> baseCost
    }
  }

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
    baseCost = 0
  }

  companion object {
    val MAPPER = mapperFor<ItemComponent>()
  }
}

val Entity.itemCmp: ItemComponent
  get() = this[ItemComponent.MAPPER]
    ?: throw GdxRuntimeException("ItemComponent for entity '$this' is null")
