package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import kotlin.random.Random

enum class ItemType {
  UNDEFINED, HAT, ROBE;

  companion object {
    private val VALUES = values()

    fun random() = VALUES[Random.nextInt(1, VALUES.size)]
  }
}

class ItemComponent : Component, Pool.Poolable {
  var itemType = ItemType.UNDEFINED
  var amount = 1

  override fun reset() {
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
