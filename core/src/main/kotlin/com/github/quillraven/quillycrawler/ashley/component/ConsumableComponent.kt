package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.command.Command
import ktx.ashley.mapperFor
import ktx.collections.GdxSet
import kotlin.reflect.KClass

class ConsumableComponent : Component, Pool.Poolable {
  val abilitiesToAdd = GdxSet<KClass<out Command>>()

  override fun reset() {
    abilitiesToAdd.clear()
  }

  companion object {
    val MAPPER = mapperFor<ConsumableComponent>()
  }
}
