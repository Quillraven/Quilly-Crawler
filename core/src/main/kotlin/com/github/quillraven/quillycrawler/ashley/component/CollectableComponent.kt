package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class CollectableComponent : Component, Pool.Poolable {

    override fun reset() {
    }

    companion object {
        val MAPPER = mapperFor<CollectableComponent>()
    }
}

val Entity.collectableCmp: CollectableComponent
    get() = this[CollectableComponent.MAPPER]
        ?: throw GdxRuntimeException("CollectableComponent for entity '$this' is null")
