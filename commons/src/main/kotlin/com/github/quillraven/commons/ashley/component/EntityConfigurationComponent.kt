package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.AbstractEntityConfiguration
import ktx.ashley.get
import ktx.ashley.mapperFor

class EntityConfigurationComponent : Component, Pool.Poolable {
    lateinit var config: AbstractEntityConfiguration

    override fun reset() {
    }

    companion object {
        val MAPPER = mapperFor<EntityConfigurationComponent>()
    }
}

/**
 * Returns an [EntityConfigurationComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.configCmp: EntityConfigurationComponent
    get() = this[EntityConfigurationComponent.MAPPER]
        ?: throw GdxRuntimeException("EntityConfigurationComponent for entity '$this' is null")
