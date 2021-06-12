package com.github.quillraven.quillycrawler.combat.command

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Constructor
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.github.quillraven.quillycrawler.combat.CombatContext
import ktx.collections.getOrPut
import kotlin.reflect.KClass

class CommandPool<T : Command>(
  private val constructor: Constructor,
  private val context: CombatContext
) : Pool<T>() {
  override fun newObject(): T {
    @Suppress("UNCHECKED_CAST")
    return constructor.newInstance(context) as T
  }
}

class CommandPools(private val combatContext: CombatContext) {
  private val pools = ObjectMap<KClass<out Command>, CommandPool<out Command>>()

  fun <T : Command> obtainCommand(entity: Entity, commandType: KClass<T>, action: T.() -> Unit = {}): T {
    @Suppress("UNCHECKED_CAST")
    val newCommand = pools.getOrPut(commandType) {
      try {
        val constructor = ClassReflection.getConstructor(commandType.java, CombatContext::class.java)
        CommandPool(constructor, combatContext)
      } catch (e: ReflectionException) {
        throw GdxRuntimeException("Could not find (CombatContext) constructor for command ${commandType.simpleName}")
      }
    }.obtain() as T
    newCommand.entity = entity
    return newCommand.apply(action)
  }

  fun freeCommand(command: Command) {
    @Suppress("UNCHECKED_CAST")
    val pool = pools.get(command::class) as CommandPool<Command>
    pool.free(command)
  }
}
