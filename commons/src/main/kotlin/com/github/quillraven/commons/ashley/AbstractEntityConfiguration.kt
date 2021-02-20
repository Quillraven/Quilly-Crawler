package com.github.quillraven.commons.ashley

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.commons.ashley.component.EntityState

abstract class AbstractEntityConfiguration(
    var size: Vector2 = Vector2(1f, 1f),
    var moveSpeed: Float = 0f,
    var atlasFilePath: String = "",
    var regionKey: String = "",
    var initialState: EntityState = EntityState.EMPTY_STATE,
    var bodyType: BodyDef.BodyType? = null,
    var boundingBoxHeightPercentage: Float = 1f,
)
