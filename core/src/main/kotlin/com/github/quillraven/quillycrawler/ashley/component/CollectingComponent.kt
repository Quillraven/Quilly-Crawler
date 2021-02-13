package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxSet

class CollectingComponent : Component, Pool.Poolable {
    val entitiesInRange = GdxSet<Entity>()

    override fun reset() {
        entitiesInRange.clear()
    }

    companion object {
        val MAPPER = mapperFor<CollectingComponent>()
    }
}

val Entity.collectingCmp: CollectingComponent
    get() = this[CollectingComponent.MAPPER]
        ?: throw GdxRuntimeException("CollectingComponent for entity '$this' is null")