package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.collections.contains
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class GearSystem :
  EntityListener,
  IteratingSystem(allOf(GearComponent::class, EquipComponent::class).exclude(RemoveComponent::class).get()) {
  private val itemFamily = allOf(ItemComponent::class).get()
  private val gearFamily = allOf(GearComponent::class).get()

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    engine.addEntityListener(itemFamily, this)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
  }

  override fun entityAdded(entity: Entity?) = Unit

  override fun entityRemoved(itemEntity: Entity) {
    val itemCmp = itemEntity.itemCmp

    engine.getEntitiesFor(gearFamily).forEach { entity ->
      val gearCmp = entity.gearCmp
      if (itemEntity == gearCmp.gear[itemCmp.gearType]) {
        // item gets removed from engine -> remove it from gear if it was equipped
        gearCmp.gear.remove(itemCmp.gearType)
      }
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val gearCmp = entity.gearCmp
    val equipCmp = entity.equipCmp
    val bagCmp = entity[BagComponent.MAPPER]

    equipCmp.removeFromGear.forEach { removeGear(gearCmp.gear, it, bagCmp?.items) }
    equipCmp.addToGear.forEach { addGear(gearCmp.gear, it, bagCmp?.items) }

    entity.remove(EquipComponent::class.java)
  }

  private fun removeGear(
    gear: ObjectMap<GearType, Entity>,
    itemToRemove: Entity,
    bag: ObjectMap<ItemType, Entity>?
  ) {
    val itemCmp = itemToRemove.itemCmp

    if (itemToRemove == gear[itemCmp.gearType]) {
      LOG.debug { "Removing gear of type '${itemCmp.gearType}': '${itemCmp.itemType}'" }
      gear.remove(itemCmp.gearType)

      // safety check to make sure that the item is still in the bag because
      // it should only be possible to equip items that are part of the entity's bag, if it has a bag
      if (bag != null && itemCmp.itemType !in bag) {
        LOG.error { "Item '$itemToRemove' of type '${itemCmp.itemType}' is not part of the bag '$bag'" }
      }
    } else {
      LOG.error { "Item '$itemToRemove' is not equipped in gear '$gear'" }
    }
  }

  private fun addGear(
    gear: ObjectMap<GearType, Entity>,
    itemToAdd: Entity,
    bag: ObjectMap<ItemType, Entity>?
  ) {
    val itemCmp = itemToAdd.itemCmp

    // safety check to make sure that the item is part of the bag because
    // it should only be possible to equip items of the entity's bag, if it has a bag
    if (bag != null && itemCmp.itemType !in bag) {
      LOG.error { "Item '$itemToAdd' of type '${itemCmp.itemType}' is not part of the bag '$bag'" }
      return
    }

    if (itemToAdd == gear[itemCmp.gearType]) {
      // gear type of item already available -> remove current item before equipping the new one
      removeGear(gear, gear[itemCmp.gearType], bag)
    }

    LOG.debug { "Adding gear of type '${itemCmp.gearType}': '${itemCmp.itemType}'" }
    gear[itemCmp.gearType] = itemToAdd
  }

  companion object {
    private val LOG = logger<GearSystem>()
  }
}
