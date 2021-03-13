package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

enum class GearType {
  UNDEFINED, HELMET, ARMOR
}

class GearComponent : Component, Pool.Poolable {
  val gear = ObjectMap<GearType, Entity>()

  override fun reset() {
    gear.clear()
  }

  companion object {
    val MAPPER = mapperFor<GearComponent>()
  }
}

val Entity.gearCmp: GearComponent
  get() = this[GearComponent.MAPPER]
    ?: throw GdxRuntimeException("GearComponent for entity '$this' is null")
