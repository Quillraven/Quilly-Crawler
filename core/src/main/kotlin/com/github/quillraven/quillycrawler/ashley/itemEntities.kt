package com.github.quillraven.quillycrawler.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.entity
import ktx.ashley.with
import ktx.collections.set

fun Engine.createItemEntity(type: ItemType, numItems: Int = 1): Entity {
  return this.entity {
    val itemCmp = with<ItemComponent> {
      itemType = type
      gearType = type.gearType
      amount = numItems
    }

    when (type) {
      ItemType.BUCKLER -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 2f
        }
        itemCmp.cost = 25
      }
      ItemType.CURSED_NECKLACE -> {
        with<StatsComponent> {
          stats[StatsType.MAX_MANA] = 35f
          stats[StatsType.INTELLIGENCE] = 10f
          stats[StatsType.MAGIC_DAMAGE] = 8f
          stats[StatsType.PHYSICAL_DAMAGE] = -4f
          stats[StatsType.PHYSICAL_ARMOR] = -3f
        }
        itemCmp.cost = 250
      }
      ItemType.HAT -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.INTELLIGENCE] = 1f
        }
        itemCmp.cost = 10
      }
      ItemType.HEALTH_POTION -> {
        with<ConsumableComponent>()
        with<StatsComponent> {
          stats[StatsType.LIFE] = 50f
        }
        itemCmp.cost = 5
      }
      ItemType.MANA_POTION -> {
        with<ConsumableComponent>()
        with<StatsComponent> {
          stats[StatsType.MANA] = 10f
        }
        itemCmp.cost = 10
      }
      ItemType.LEATHER_BOOTS -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.AGILITY] = 1f
        }
        itemCmp.cost = 30
      }
      ItemType.LEATHER_GLOVES -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.STRENGTH] = 1f
        }
        itemCmp.cost = 30
      }
      ItemType.ROBE -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 2f
          stats[StatsType.INTELLIGENCE] = 3f
        }
        itemCmp.cost = 35
      }
      ItemType.ROD -> {
        with<StatsComponent> {
          stats[StatsType.MAGIC_DAMAGE] = 3f
          stats[StatsType.PHYSICAL_DAMAGE] = 1f
        }
        itemCmp.cost = 40
      }
      ItemType.UNDEFINED -> {
      }
    }
  }
}
