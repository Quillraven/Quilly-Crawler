package com.github.quillraven.commons.ashley

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.commons.ashley.component.*

/**
 * Abstract entity configuration to configure the values of an entity. It is used by
 * [AbstractEntityFactory] to create an [Entity] out of it.
 *
 * Use [size] to configure the [TransformComponent.size].
 *
 * Use [moveSpeed] to define the maximum move speed of your entity. This is not supported by the commons library
 * as it is different for each game. Use it for your own MoveComponent.
 *
 * Use [atlasFilePath] and [regionKey] to define the animation of the [AnimationComponent].
 *
 * Use [initialState] to set the first state of the [StateComponent].
 *
 * Use [bodyType] and [boundingBoxHeightPercentage] to configure the body of the [Box2DComponent].
 */
abstract class AbstractEntityConfiguration(
  var size: Vector2 = Vector2(1f, 1f),
  var moveSpeed: Float = 0f,
  var atlasFilePath: String = "",
  var regionKey: String = "",
  var initialState: EntityState = EntityState.EMPTY_STATE,
  var bodyType: BodyDef.BodyType? = null,
  var boundingBoxHeightPercentage: Float = 1f,
) {
  override fun toString(): String {
    return "AbstractEntityConfiguration(" +
      "size=$size, " +
      "moveSpeed=$moveSpeed, " +
      "atlasFilePath='$atlasFilePath', " +
      "regionKey='$regionKey', " +
      "initialState=$initialState, " +
      "bodyType=$bodyType, " +
      "boundingBoxHeightPercentage=$boundingBoxHeightPercentage)"
  }
}
