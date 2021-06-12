package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.entity
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.ashley.with
import ktx.collections.GdxArray

class HealEmitterComponent : Component, Pool.Poolable {
  lateinit var source: Entity
  lateinit var target: Entity
  var life: Float = 0f
  var mana: Float = 0f
  var delay: Float = 0f

  override fun reset() {
    life = 0f
    mana = 0f
    delay = 0f
  }

  companion object {
    val MAPPER = mapperFor<HealEmitterComponent>()
  }
}

val Entity.healEmitterCmp: HealEmitterComponent
  get() = this[HealEmitterComponent.MAPPER]
    ?: throw GdxRuntimeException("HealEmitterComponent for entity '$this' is null")

fun Entity.heal(
  engine: Engine,
  targets: GdxArray<Entity>,
  life: Float,
  mana: Float,
  delay: Float = 0f
) {
  val sourceEntity = this

  targets.forEach { targetEntity ->
    engine.entity {
      with<HealEmitterComponent> {
        this.source = sourceEntity
        this.target = targetEntity
        this.life = life
        this.mana = mana
        this.delay = delay
      }
    }
  }
}
