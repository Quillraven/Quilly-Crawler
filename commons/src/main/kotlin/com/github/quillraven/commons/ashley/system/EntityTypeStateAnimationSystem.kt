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
        val entityTypeCmp = entity.entityTypeCmp.type
        val stateCmp = entity.stateCmp
        val animationCmp = entity.animationCmp

        if (animationCmp.atlasAsset == entityTypeCmp.atlasAsset && stateCmp.state == stateCmp.stateMachine.currentState) {
            // animation is already up to date -> do nothing
            return
        }

        animationCmp.run {
            atlasAsset = entityTypeCmp.atlasAsset
            regionKey = regionStringCache
                .getOrPut(entityTypeCmp) { ObjectMap() }
                .getOrPut(stateCmp.state) {
                    val result = "${entityTypeCmp.regionKey}-${stateCmp.state.toString().toLowerCase()}"
                    LOG.debug { "Caching animation string '$result'" }
                    result
                }
        }
    }

    companion object {
        private val LOG = logger<EntityTypeStateAnimationSystem>()
    }
}