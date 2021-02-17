package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.Box2DComponent
import com.github.quillraven.quillycrawler.ashley.component.MoveComponent
import com.github.quillraven.quillycrawler.ashley.component.moveCmp
import ktx.ashley.allOf
import ktx.ashley.get
import kotlin.math.max
import kotlin.math.min

class MoveSystem : IteratingSystem(allOf(MoveComponent::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val moveCmp = entity.moveCmp

        moveCmp.alpha = max(0f, min(1f, moveCmp.alpha + deltaTime))
        moveCmp.speed = moveCmp.accInterpolation.apply(0f, moveCmp.maxSpeed, moveCmp.alpha)

        // calculate impulse to apply
        val box2dCmp = entity[Box2DComponent.MAPPER]
        if (box2dCmp != null) {
            with(box2dCmp.body) {
                box2dCmp.impulse.x = mass * (moveCmp.speed * moveCmp.cosDeg - linearVelocity.x)
                box2dCmp.impulse.y = mass * (moveCmp.speed * moveCmp.sinDeg - linearVelocity.y)
            }
        }
    }
}