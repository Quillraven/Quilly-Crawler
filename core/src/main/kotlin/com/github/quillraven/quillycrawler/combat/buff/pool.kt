package com.github.quillraven.quillycrawler.combat.buff

import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Constructor
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.github.quillraven.quillycrawler.combat.CombatContext
import ktx.collections.getOrPut
import kotlin.reflect.KClass

class BuffPool<T : Buff>(
  private val constructor: Constructor,
  private val context: CombatContext
) : Pool<T>() {
  override fun newObject(): T {
    @Suppress("UNCHECKED_CAST")
    return constructor.newInstance(context) as T
  }
}

class BuffPools(private val combatContext: CombatContext) {
  private val pools = ObjectMap<KClass<out Buff>, BuffPool<out Buff>>()

  fun <T : Buff> obtainBuff(buffType: KClass<T>, action: T.() -> Unit): T {
    @Suppress("UNCHECKED_CAST")
    val newBuff = pools.getOrPut(buffType) {
      try {
        val constructor = ClassReflection.getConstructor(buffType.java, CombatContext::class.java)
        BuffPool(constructor, combatContext)
      } catch (e: ReflectionException) {
        throw GdxRuntimeException("Could not find (CombatContext) constructor for buff ${buffType.simpleName}")
      }
    }.obtain() as T
    return newBuff.apply(action)
  }

  fun freeBuff(buff: Buff) {
    @Suppress("UNCHECKED_CAST")
    val pool = pools.get(buff::class) as BuffPool<Buff>
    pool.free(buff)
  }
}
