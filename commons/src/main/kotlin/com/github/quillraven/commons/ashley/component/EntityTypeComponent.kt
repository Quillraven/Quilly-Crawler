package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

// TODO replace with entity cfg DSL?
interface IEntityType {
    val atlasFilePath: String
    val regionKey: String
}

class EntityTypeComponent : Component, Pool.Poolable {
    var type = EMPTY_ENTITY_TYPE

    override fun reset() {
        type = EMPTY_ENTITY_TYPE
    }

    companion object {
        val MAPPER = mapperFor<EntityTypeComponent>()
        val EMPTY_ENTITY_TYPE = object : IEntityType {
            override val atlasFilePath = ""
            override val regionKey = ""
        }
    }
}

val Entity.entityTypeCmp: EntityTypeComponent
    get() = this[EntityTypeComponent.MAPPER]
        ?: throw GdxRuntimeException("EntityTypeComponent for entity '$this' is null")
