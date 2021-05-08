package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.buff.Buff
import ktx.ashley.get
import ktx.ashley.mapperFor
import kotlin.reflect.KClass

class BuffComponent : Component, Pool.Poolable {
  var buffType: KClass<out Buff> = Buff::class
  lateinit var buff: Buff
  lateinit var entity: Entity

  override fun reset() {
    buffType = Buff::class
  }

  companion object {
    val MAPPER = mapperFor<BuffComponent>()
  }
}

val Entity.buffCmp: BuffComponent
  get() = this[BuffComponent.MAPPER]
    ?: throw GdxRuntimeException("BuffComponent for entity '$this' is null")
