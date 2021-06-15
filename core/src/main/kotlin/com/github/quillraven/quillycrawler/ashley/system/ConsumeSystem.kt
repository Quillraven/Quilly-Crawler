package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.event.CombatConsumeItemEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class ConsumeSystem(private val eventDispatcher: GameEventDispatcher) :
  IteratingSystem(allOf(ConsumeComponent::class).exclude(RemoveComponent::class).get()) {
  override fun processEntity(consumerEntity: Entity, deltaTime: Float) {
    with(consumerEntity.consumeCmp) {
      itemsToConsume.forEach { itemEntity ->
        val itemStatsCmp = itemEntity[StatsComponent.MAPPER]
        val itemCmp = itemEntity.itemCmp
        if (itemStatsCmp == null) {
          LOG.error { "Trying to consume item '${itemCmp.itemType}' that does nothing" }
          return@forEach
        }

        // consume item and add stats to entity which is consuming the item
        consumerEntity[StatsComponent.MAPPER]?.let { consumerStatsCmp ->
          itemStatsCmp.stats.forEach { itemStat ->
            when (itemStat.key) {
              StatsType.LIFE -> {
                consumerStatsCmp[StatsType.LIFE] =
                  (consumerStatsCmp[StatsType.LIFE] + itemStat.value).coerceAtMost(consumerStatsCmp[StatsType.MAX_LIFE])
              }
              StatsType.MANA -> {
                consumerStatsCmp[StatsType.MANA] =
                  (consumerStatsCmp[StatsType.MANA] + itemStat.value).coerceAtMost(consumerStatsCmp[StatsType.MAX_MANA])
              }
              else -> {
                consumerStatsCmp[itemStat.key] = consumerStatsCmp[itemStat.key] + itemStat.value
              }
            }
          }
        }

        eventDispatcher.dispatchEvent<CombatConsumeItemEvent> {
          this.entity = consumerEntity
          this.statsCmp = itemStatsCmp
        }

        // reduce amount and remove the item if necessary
        itemCmp.amount--
        if (itemCmp.amount <= 0) {
          LOG.debug { "Item '${itemCmp.itemType}' is completely consumed and gets removed now" }
          consumerEntity[BagComponent.MAPPER]?.items?.remove(itemCmp.itemType)
          itemEntity.removeFromEngine(engine)
        }
      }
    }

    consumerEntity.remove(ConsumeComponent::class.java)
  }

  companion object {
    private val LOG = logger<ConsumeSystem>()
  }
}
