package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.allOf
import ktx.ashley.entity
import ktx.ashley.exclude
import ktx.ashley.with
import ktx.collections.contains
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger
import kotlin.random.Random

class LootSystem : EntityListener, IteratingSystem(
  allOf(PlayerComponent::class, LootComponent::class, BagComponent::class).exclude(RemoveComponent::class).get()
) {
  private val bagFamily = allOf(BagComponent::class).get()

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    engine.addEntityListener(bagFamily, this)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
  }

  override fun entityAdded(entity: Entity) = Unit

  override fun entityRemoved(entity: Entity) {
    // remove all item entities when an entity with a bag gets removed
    entity.bagCmp.items.values().forEach { itemEntity ->
      itemEntity.removeFromEngine(engine)
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    when (entity.lootCmp.lootType) {
      LootType.COMMON -> {
        addPlayerLoot(entity, Random.nextInt(0, 11), 25)
      }
      LootType.RARE -> {
        addPlayerLoot(entity, Random.nextInt(25, 51), 50)
      }
      LootType.EPIC -> {
        addPlayerLoot(entity, Random.nextInt(100, 201), 100, Random.nextInt(1, 3))
      }
      else -> {
        LOG.error { "Undefined loot for entity '$entity'" }
      }
    }

    entity.remove(LootComponent::class.java)
  }

  private fun addPlayerLoot(entity: Entity, goldCoins: Int, itemChance: Int, numItems: Int = 1) {
    with(entity.bagCmp) {
      gold += goldCoins
      LOG.debug { "Added '$goldCoins' gold. Total is '$gold'" }

      if (itemChance >= 100 || Random.nextInt(1, 101) <= itemChance) {
        for (i in 0 until numItems) {
          createItemForBag(this)
        }
      }
    }
  }

  private fun createItemForBag(bagCmp: BagComponent) {
    val type = ItemType.random()

    if (type in bagCmp.items) {
      bagCmp.items[type].itemCmp.amount++
    } else {
      bagCmp.items[type] = engine.entity {
        with<ItemComponent> {
          itemType = type
          gearType = type.gearType
          amount = 1
        }

        when (type) {
          ItemType.HAT -> {
            with<StatsComponent> {
              stats[StatsType.PHYSICAL_ARMOR] = 1f
              stats[StatsType.INTELLIGENCE] = 1f
            }
          }
          ItemType.ROBE -> {
            with<StatsComponent> {
              stats[StatsType.PHYSICAL_ARMOR] = 3f
              stats[StatsType.INTELLIGENCE] = 3f
            }
          }
          else -> {
            LOG.error { "Unsupported ItemType '$type'" }
          }
        }
      }
    }

    LOG.debug {
      "Added item of type '$type' to bag: ${
        bagCmp.items.entries().joinToString(", ") { "${it.value.itemCmp.amount}x ${it.key.name}" }
      }"
    }
  }

  companion object {
    private val LOG = logger<LootSystem>()
  }
}
