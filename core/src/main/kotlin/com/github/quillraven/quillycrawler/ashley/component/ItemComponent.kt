package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
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

  override fun reset() {
    gearType = GearType.UNDEFINED
    itemType = ItemType.UNDEFINED
    amount = 1
  }

  companion object {
    val MAPPER = mapperFor<ItemComponent>()
  }
}

val Entity.itemCmp: ItemComponent
  get() = this[ItemComponent.MAPPER]
    ?: throw GdxRuntimeException("ItemComponent for entity '$this' is null")
