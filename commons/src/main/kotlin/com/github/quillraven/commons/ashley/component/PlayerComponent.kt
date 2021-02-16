package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

/**
 * Component to identify a player [Entity]. Can be used for [iterating systems][IteratingSystem] to
 * only run them for player entities.
 */
class PlayerComponent : Component, Pool.Poolable {
    override fun reset() = Unit

    companion object {
        val MAPPER = mapperFor<PlayerComponent>()
    }
}
