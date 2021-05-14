package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Queue
import com.github.quillraven.quillycrawler.combat.command.Command
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.contains
import kotlin.reflect.KClass

class CombatComponent : Component, Pool.Poolable {
  val availableCommands = ObjectMap<KClass<out Command>, Command>()
  val learnedCommands = GdxSet<KClass<out Command>>()
  val commandsToExecute = Queue<Command>()

  inline fun <reified T : Command> command(): T {
    return availableCommands.get(T::class) as T
  }

  inline fun <reified T : Command> learn() {
    learnedCommands.add(T::class)
  }

  fun addCommand(command: Command, target: Entity) {
    commandsToExecute.addFirst(command.apply { targets.add(target) })
  }

  override fun reset() {
    availableCommands.clear()
    learnedCommands.clear()
    commandsToExecute.clear()
  }

  companion object {
    val MAPPER = mapperFor<CombatComponent>()
  }
}

val Entity.combatCmp: CombatComponent
  get() = this[CombatComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatComponent for entity '$this' is null")

inline fun <reified T : Command> Entity.addCommand(targets: GdxArray<Entity>) {
  with(this.combatCmp) {
    if (T::class in availableCommands) {
      commandsToExecute.addFirst(command<T>().apply {
        this.targets.addAll(targets)
      })
    }
  }
}

inline fun <reified T : Command> Entity.addCommand(target: Entity? = null) {
  with(this.combatCmp) {
    if (T::class in availableCommands) {
      commandsToExecute.addFirst(command<T>().apply {
        if (target != null) {
          this.targets.addAll(target)
        }
      })
    }
  }
}
