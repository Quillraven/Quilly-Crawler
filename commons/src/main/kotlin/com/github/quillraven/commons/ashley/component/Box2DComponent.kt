package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.Box2DSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to store Box2D physic related data. It is used for the [Box2DSystem].
 *
 * A [body] must be assigned when creating an instance of this component otherwise the
 * [Box2DSystem] will fail.
 *
 * Use [renderPosition] for rendering. It represents an interpolated position of the [body]
 * between its previous and current position and is automatically calculated and set by
 * the [Box2DSystem].
 *
 * Use [impulse] to define the impulse that gets applied to the [body] before a call to
 * [World.step].
 * The [body] gets automatically destroyed when the component's [reset][Pool.Poolable.reset]
 * function is called.
 *
 * Use [stopMovementImmediately] to set an [impulse] that will stop the body during the next step of the world.
 *
 * Use [box2dCmp] to easily access the [Box2DComponent] of an [Entity]. Only use it if you are sure
 * that the component is not null. Otherwise, it will throw a [GdxRuntimeException].
 */
class Box2DComponent : Component, Pool.Poolable {
  lateinit var body: Body
  val renderPosition = Vector2()
  val impulse = Vector2()

  fun stopMovementImmediately() {
    impulse.x = body.mass * (0f - body.linearVelocity.x)
    impulse.y = body.mass * (0f - body.linearVelocity.y)
  }

  override fun reset() {
    body.world.destroyBody(body)
    body.userData = null
    renderPosition.set(0f, 0f)
    impulse.set(0f, 0f)
  }

  companion object {
    val MAPPER = mapperFor<Box2DComponent>()
    val TMP_VECTOR2 = Vector2()
  }
}

/**
 * Returns a [Box2DComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.box2dCmp: Box2DComponent
  get() = this[Box2DComponent.MAPPER]
    ?: throw GdxRuntimeException("Box2DComponent for entity '$this' is null")
