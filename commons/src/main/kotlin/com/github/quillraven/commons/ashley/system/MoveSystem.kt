package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.MoveComponent
import com.github.quillraven.commons.ashley.component.moveCmp
import ktx.ashley.allOf
import kotlin.math.max
import kotlin.math.min

class MoveSystem : IteratingSystem(allOf(MoveComponent::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val moveCmp = entity.moveCmp

        moveCmp.alpha = max(0f, min(1f, moveCmp.alpha + deltaTime))
        moveCmp.speed = moveCmp.accInterpolation.apply(0f, moveCmp.maxSpeed, moveCmp.alpha)
    }
}