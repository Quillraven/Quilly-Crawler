package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf

class EquipComponent : Component, Pool.Poolable {
  val addToGear = gdxArrayOf<Entity>()
  val removeFromGear = gdxArrayOf<Entity>()

  override fun reset() {
    addToGear.clear()
    removeFromGear.clear()
  }

  companion object {
    val MAPPER = mapperFor<EquipComponent>()
  }
}

val Entity.equipCmp: EquipComponent
  get() = this[EquipComponent.MAPPER]
    ?: throw GdxRuntimeException("EquipComponent for entity '$this' is null")
