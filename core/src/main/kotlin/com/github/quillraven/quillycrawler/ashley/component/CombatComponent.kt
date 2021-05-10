package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.combat.command.CommandRequest
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxArray
import kotlin.reflect.KClass

class CombatComponent : Component, Pool.Poolable {
  val commandRequests = GdxArray<CommandRequest>()

  override fun reset() {
    commandRequests.clear()
  }

  companion object {
    val MAPPER = mapperFor<CombatComponent>()
    val REQUEST_POOL = object : Pool<CommandRequest>() {
      override fun newObject() = CommandRequest()
    }
  }
}

val Entity.combatCmp: CombatComponent
  get() = this[CombatComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatComponent for entity '$this' is null")

fun Entity.addCommandRequest(type: KClass<out Command>, target: Entity? = null) {
  this.combatCmp.commandRequests.add(CombatComponent.REQUEST_POOL.obtain().apply {
    this.type = type
    if (target != null) {
      this.targets.add(target)
    }
  })
}
