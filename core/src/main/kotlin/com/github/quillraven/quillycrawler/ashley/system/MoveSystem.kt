package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.Box2DComponent
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.quillycrawler.ashley.component.MoveComponent
import com.github.quillraven.quillycrawler.ashley.component.moveCmp
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import kotlin.math.max
import kotlin.math.min

class MoveSystem : IteratingSystem(allOf(MoveComponent::class).exclude(RemoveComponent::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        with(entity.moveCmp) {
            if (root) {
                entity[Box2DComponent.MAPPER]?.stopMovementImmediately()
            } else {
                alpha = max(0f, min(1f, alpha + deltaTime))
                speed = accInterpolation.apply(0f, maxSpeed, alpha)

                // calculate impulse to apply
                entity[Box2DComponent.MAPPER]?.let { box2dCmp ->
                    with(box2dCmp.body) {
                        box2dCmp.impulse.x = mass * (speed * cosDeg - linearVelocity.x)
                        box2dCmp.impulse.y = mass * (speed * sinDeg - linearVelocity.y)
                    }
                }
            }
        }
    }
}