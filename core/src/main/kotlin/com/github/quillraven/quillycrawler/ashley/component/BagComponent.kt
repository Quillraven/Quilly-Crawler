package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class BagComponent : Component, Pool.Poolable {
  var gold = 0
  val items = ObjectMap<ItemType, Entity>()

  override fun reset() {
    items.clear()
  }

  companion object {
    val MAPPER = mapperFor<BagComponent>()
  }
}

val Entity.bagCmp: BagComponent
  get() = this[BagComponent.MAPPER]
    ?: throw GdxRuntimeException("BagComponent for entity '$this' is null")
