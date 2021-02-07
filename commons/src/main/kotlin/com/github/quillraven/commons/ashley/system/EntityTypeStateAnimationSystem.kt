package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.component.*
import ktx.ashley.allOf
import ktx.log.debug
import ktx.log.logger

class EntityTypeStateAnimationSystem :
    IteratingSystem(allOf(EntityTypeComponent::class, StateComponent::class, AnimationComponent::class).get()) {
    private val regionStringCache = ObjectMap<IEntityType, ObjectMap<IState, String>>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val entityType = entity.entityType.type
        val state = entity.state
        val animation = entity.animation

        if (animation.atlasAsset == entityType.atlasAsset && state.state == state.stateMachine.currentState) {
            // animation is already up to date -> do nothing
            return
        }

        animation.run {
            atlasAsset = entityType.atlasAsset
            regionKey = regionStringCache
                .getOrPut(entityType) { ObjectMap() }
                .getOrPut(state.state) {
                    val result = "${entityType.regionKey}-${state.state.toString().toLowerCase()}"
                    LOG.debug { "Caching animation string '$result'" }
                    result
                }
        }
    }

    companion object {
        private val LOG = logger<EntityTypeStateAnimationSystem>()
    }
}