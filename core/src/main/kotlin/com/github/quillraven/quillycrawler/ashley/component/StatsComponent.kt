package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.set

enum class StatsType {
  LIFE, MAX_LIFE,
  MANA, MAX_MANA,
  STRENGTH, AGILITY, INTELLIGENCE,
  PHYSICAL_DAMAGE, MAGIC_DAMAGE,
  PHYSICAL_ARMOR, MAGIC_ARMOR;

  companion object {
    val VALUES = values()
  }
}

class StatsComponent : Component, Pool.Poolable {
  val stats = ObjectMap<StatsType, Float>()

  operator fun get(type: StatsType): Float = stats.get(type, 0f)

  operator fun set(type: StatsType, value: Float) {
    stats[type] = value
  }

  override fun reset() {
    stats.clear()
  }

  fun totalStatValue(entity: Entity, type: StatsType): Float {
    val baseValue = stats.get(type, 0f)

    val gearComponent = entity[GearComponent.MAPPER]
    return if (gearComponent == null || gearComponent.gear.isEmpty) {
      baseValue
    } else {
      var gearValue = 0f

      gearComponent.gear.values().forEach { gear ->
        gear[MAPPER]?.let { gearStats ->
          gearValue += gearStats.stats.get(type, 0f)
        }
      }

      baseValue + gearValue
    }
  }

  companion object {
    val MAPPER = mapperFor<StatsComponent>()
  }
}

fun Entity.totalStatValue(type:StatsType): Float = this.statsCmp.totalStatValue(this,type)

val Entity.statsCmp: StatsComponent
  get() = this[StatsComponent.MAPPER]
    ?: throw GdxRuntimeException("StatsComponent for entity '$this' is null")
