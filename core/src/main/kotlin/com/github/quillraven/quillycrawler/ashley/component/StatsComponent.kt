package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import java.util.*

enum class StatsType {
  LIFE, MAX_LIFE,
  MANA, MAX_MANA,
  STRENGTH, AGILITY, INTELLIGENCE,
  PHYSICAL_DAMAGE, MAGIC_DAMAGE,
  PHYSICAL_ARMOR, MAGIC_ARMOR
}

class StatsComponent : Component, Pool.Poolable {
  val stats = EnumMap<StatsType, Float>(StatsType::class.java)

  override fun reset() {
    stats.clear()
  }

  companion object {
    val MAPPER = mapperFor<StatsComponent>()
  }
}

val Entity.statsCmp: StatsComponent
  get() = this[StatsComponent.MAPPER]
    ?: throw GdxRuntimeException("StatsComponent for entity '$this' is null")
