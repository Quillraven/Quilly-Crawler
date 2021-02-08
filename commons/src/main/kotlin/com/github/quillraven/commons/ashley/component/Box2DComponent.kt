package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class Box2DComponent : Component, Pool.Poolable {
    lateinit var body: Body
    val renderPosition = Vector2()
    val impulse = Vector2()

    override fun reset() {
        body.world.destroyBody(body)
        body.userData = null
        renderPosition.set(0f, 0f)
        impulse.set(0f, 0f)
    }

    companion object {
        val MAPPER = mapperFor<Box2DComponent>()
    }
}

val Entity.box2dCmp: Box2DComponent
    get() = this[Box2DComponent.MAPPER]
        ?: throw GdxRuntimeException("Box2DComponent for entity '$this' is null")
