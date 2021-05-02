package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class DamageEmitterComponent : Component, Pool.Poolable {
  lateinit var source: Entity
  lateinit var target: Entity
  var physicalDamage: Float = 0f
  var magicDamage: Float = 0f
  var damageDelay: Float = 0f

  override fun reset() {
    damageDelay = 0f
    physicalDamage = 0f
    magicDamage = 0f
  }

  companion object {
    val MAPPER = mapperFor<DamageEmitterComponent>()
  }
}

val Entity.damageEmitterCmp: DamageEmitterComponent
  get() = this[DamageEmitterComponent.MAPPER]
    ?: throw GdxRuntimeException("DamageEmitterComponent for entity '$this' is null")
