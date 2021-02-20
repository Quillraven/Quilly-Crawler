package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.AbstractEntityConfiguration
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.collections.getOrPut
import ktx.ashley.allOf
import ktx.log.debug
import ktx.log.logger

class EntityTypeStateAnimationSystem :
    IteratingSystem(
        allOf(
            EntityConfigurationComponent::class,
            StateComponent::class,
            AnimationComponent::class
        ).get()
    ) {
    private val regionStringCache = ObjectMap<AbstractEntityConfiguration, ObjectMap<EntityState, String>>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val configCmp = entity.configCmp
        val stateCmp = entity.stateCmp
        val animationCmp = entity.animationCmp

        if (stateCmp.state == EntityState.EMPTY_STATE) {
            // state is not changing -> nothing to do
            return
        }

        animationCmp.run {
            atlasFilePath = configCmp.config.atlasFilePath
            regionKey = regionStringCache
                .getOrPut(configCmp.config) { ObjectMap() }
                .getOrPut(stateCmp.state) {
                    val result = "${configCmp.config.regionKey}/${stateCmp.state.toString().toLowerCase()}"
                    LOG.debug { "Caching animation string '$result'" }
                    result
                }
        }
    }

    companion object {
        private val LOG = logger<EntityTypeStateAnimationSystem>()
    }
}