package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.assets.ITextureAtlasAssets
import ktx.ashley.get
import ktx.ashley.mapperFor

interface IEntityType {
    val atlasAsset: ITextureAtlasAssets
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
            override val atlasAsset = ITextureAtlasAssets.EMPTY_TEXTURE_ATLAS_ASSET
            override val regionKey = ""
        }
    }
}

val Entity.entityType: EntityTypeComponent
    get() = this[EntityTypeComponent.MAPPER]
        ?: throw GdxRuntimeException("EntityTypeComponent for entity '$this' is null")
