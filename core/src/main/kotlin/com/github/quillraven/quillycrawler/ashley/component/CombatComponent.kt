package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Queue
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.combat.command.CommandDeath
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.contains
import kotlin.reflect.KClass

class CombatComponent : Component, Pool.Poolable {
  val availableCommands = ObjectMap<KClass<out Command>, Command>()
  val commandsToLearn = GdxSet<KClass<out Command>>()
  private val commandsToExecute = Queue<Command>()

  inline fun <reified T : Command> command(): T {
    return availableCommands.get(T::class) as T
  }

  inline fun <reified T : Command> learn() {
    commandsToLearn.add(T::class)
  }

  fun addCommand(command: Command, targets: GdxArray<Entity>) {
    commandsToExecute.addFirst(command.apply { this.targets.addAll(targets) })
  }

  fun addCommand(command: Command, target: Entity? = null) {
    commandsToExecute.addFirst(command.apply {
      if (target != null) {
        this.targets.addAll(target)
      }
    })
  }

  fun hasNoCommands(): Boolean = commandsToExecute.isEmpty

  fun hasDeathCommand(): Boolean {
    commandsToExecute.forEach {
      if (it is CommandDeath) {
        return true
      }
    }
    return false
  }

  fun forEachCommand(action: (Command) -> Unit) {
    commandsToExecute.forEach { it.run(action) }
  }

  fun clearCommands() = commandsToExecute.clear()

  override fun reset() {
    availableCommands.clear()
    commandsToLearn.clear()
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
      addCommand(command<T>(), targets)
    }
  }
}

inline fun <reified T : Command> Entity.addCommand(target: Entity? = null) {
  with(this.combatCmp) {
    if (T::class in availableCommands) {
      addCommand(command<T>(), target)
    }
  }
}
