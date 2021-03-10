package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

enum class LootType {
  UNDEFINED, COMMON, RARE, EPIC
}

class LootComponent : Component, Pool.Poolable {
  var lootType = LootType.UNDEFINED

  override fun reset() {
    lootType = LootType.UNDEFINED
  }

  companion object {
    val MAPPER = mapperFor<LootComponent>()
  }
}

val Entity.lootCmp: LootComponent
  get() = this[LootComponent.MAPPER]
    ?: throw GdxRuntimeException("LootComponent for entity '$this' is null")
