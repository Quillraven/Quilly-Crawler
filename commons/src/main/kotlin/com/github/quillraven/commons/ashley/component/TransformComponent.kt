package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

const val Z_DEFAULT = 0

/**
 * Component to store transformation related data.
 *
 * Use [position] to set the position of an [Entity]. The z coordinate is used for sorting the entities
 * during render. If they are on the same layer (=same z value) then the y coordinate is used.
 *
 * Use [size] to set the width and height of an [Entity]. The values are in world units instead of pixels.
 * The size also defines the size of the [Sprite] if an entity has a [RenderComponent].
 *
 * Use [transformCmp] to easily access the [TransformComponent] of an [Entity]. Only use it if you are sure that
 * the component is not null. Otherwise, it will throw a [GdxRuntimeException].
 */
class TransformComponent : Component, Pool.Poolable, Comparable<TransformComponent> {
    val position = Vector3()
    val size = Vector2(1f, 1f)

    override fun reset() {
        position.set(0f, 0f, Z_DEFAULT.toFloat())
        size.set(1f, 1f)
    }

    override fun compareTo(other: TransformComponent): Int {
        val zDiff = other.position.z.compareTo(position.z)
        return if (zDiff == 0) other.position.y.compareTo(position.y) else zDiff
    }

    companion object {
        val MAPPER = mapperFor<TransformComponent>()
    }
}

/**
 * Returns a [TransformComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.transformCmp: TransformComponent
    get() = this[TransformComponent.MAPPER]
        ?: throw GdxRuntimeException("TransformComponent for entity '$this' is null")
