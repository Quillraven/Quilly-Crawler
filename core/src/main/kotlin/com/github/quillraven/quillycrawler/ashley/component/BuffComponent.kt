package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.buff.Buff
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxSet
import kotlin.reflect.KClass

class BuffComponent : Component, Pool.Poolable {
  val buffsToAdd = GdxSet<KClass<out Buff>>()
  val buffs = ObjectMap<KClass<out Buff>, Buff>()

  override fun reset() {
    buffsToAdd.clear()
    buffs.clear()
  }

  companion object {
    val MAPPER = mapperFor<BuffComponent>()
  }
}

val Entity.buffCmp: BuffComponent
  get() = this[BuffComponent.MAPPER]
    ?: throw GdxRuntimeException("BuffComponent for entity '$this' is null")

fun Entity.addBuff(buffType: KClass<out Buff>) {
  this[BuffComponent.MAPPER]?.buffsToAdd?.add(buffType)
}
