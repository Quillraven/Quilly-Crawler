package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.createItemEntity
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
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
    entity[BagComponent.MAPPER]?.let { bagCmp ->
      bagCmp.items.values().forEach { itemEntity ->
        itemEntity.removeFromEngine(engine)
      }
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val lootCmp = entity.lootCmp

    LOG.debug { "Process '${lootCmp.lootType}' loot" }

    when (lootCmp.lootType) {
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
    val type = ItemType.randomGearItem()

    if (type in bagCmp.items) {
      bagCmp.items[type].itemCmp.amount++
    } else {
      bagCmp.items[type] = engine.createItemEntity(type)
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
