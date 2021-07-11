package com.github.quillraven.quillycrawler.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.combat.command.CommandExplosion
import com.github.quillraven.quillycrawler.combat.command.CommandFirebolt
import com.github.quillraven.quillycrawler.combat.command.CommandHeal
import com.github.quillraven.quillycrawler.combat.command.CommandProtect
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
        itemCmp.baseCost = 25
      }
      ItemType.CURSED_NECKLACE -> {
        with<StatsComponent> {
          stats[StatsType.MAX_MANA] = 35f
          stats[StatsType.INTELLIGENCE] = 10f
          stats[StatsType.MAGIC_DAMAGE] = 8f
          stats[StatsType.PHYSICAL_DAMAGE] = -4f
          stats[StatsType.PHYSICAL_ARMOR] = -3f
        }
        itemCmp.baseCost = 250
      }
      ItemType.HAT -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.INTELLIGENCE] = 1f
        }
        itemCmp.baseCost = 10
      }
      ItemType.HEALTH_POTION -> {
        with<ConsumableComponent>()
        with<StatsComponent> {
          stats[StatsType.LIFE] = 50f
        }
        itemCmp.baseCost = 5
      }
      ItemType.MANA_POTION -> {
        with<ConsumableComponent>()
        with<StatsComponent> {
          stats[StatsType.MANA] = 10f
        }
        itemCmp.baseCost = 10
      }
      ItemType.LEATHER_BOOTS -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.AGILITY] = 1f
        }
        itemCmp.baseCost = 30
      }
      ItemType.LEATHER_GLOVES -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.STRENGTH] = 1f
        }
        itemCmp.baseCost = 30
      }
      ItemType.ROBE -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 2f
          stats[StatsType.INTELLIGENCE] = 3f
        }
        itemCmp.baseCost = 35
      }
      ItemType.ROD -> {
        with<StatsComponent> {
          stats[StatsType.MAGIC_DAMAGE] = 3f
          stats[StatsType.PHYSICAL_DAMAGE] = 1f
        }
        itemCmp.baseCost = 40
      }
      ItemType.TOME_EXPLOSION -> {
        with<ConsumableComponent> { abilitiesToAdd.add(CommandExplosion::class) }
        itemCmp.baseCost = 400
      }
      ItemType.TOME_FIREBOLT -> {
        with<ConsumableComponent> { abilitiesToAdd.add(CommandFirebolt::class) }
        itemCmp.baseCost = 100
      }
      ItemType.TOME_HEAL -> {
        with<ConsumableComponent> { abilitiesToAdd.add(CommandHeal::class) }
        itemCmp.baseCost = 200
      }
      ItemType.TOME_PROTECT -> {
        with<ConsumableComponent> { abilitiesToAdd.add(CommandProtect::class) }
        itemCmp.baseCost = 150
      }
      ItemType.TOME_STRENGTH -> {
        with<ConsumableComponent>()
        with<StatsComponent> { stats[StatsType.STRENGTH] = 1f }
        itemCmp.baseCost = 100
      }
      ItemType.TOME_AGILITY -> {
        with<ConsumableComponent>()
        with<StatsComponent> { stats[StatsType.AGILITY] = 1f }
        itemCmp.baseCost = 100
      }
      ItemType.TOME_INTELLIGENCE -> {
        with<ConsumableComponent>()
        with<StatsComponent> { stats[StatsType.INTELLIGENCE] = 1f }
        itemCmp.baseCost = 100
      }
      ItemType.UNDEFINED -> {
      }
    }
  }
}
