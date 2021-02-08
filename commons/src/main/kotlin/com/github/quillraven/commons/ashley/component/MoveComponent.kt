package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class MoveComponent : Component, Pool.Poolable {
    var speed = 0f
    var maxSpeed = 1f
    var alpha = 0f
    var accInterpolation: Interpolation = Interpolation.exp10Out
    var directionDeg = 0f

    override fun reset() {
        speed = 0f
        maxSpeed = 1f
        alpha = 0f
        accInterpolation = Interpolation.exp10Out
        directionDeg = 0f
    }

    companion object {
        val MAPPER = mapperFor<MoveComponent>()
    }
}

val Entity.moveCmp: MoveComponent
    get() = this[MoveComponent.MAPPER]
        ?: throw GdxRuntimeException("MoveComponent for entity '$this' is null")
