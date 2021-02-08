package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.cosDeg
import com.badlogic.gdx.math.MathUtils.sinDeg
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.commons.ashley.component.*
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.log.error
import ktx.log.logger
import kotlin.math.min

class Box2DSystem(
    private val world: World,
    private val physicTimeStep: Float
) : IteratingSystem(allOf(Box2DComponent::class, TransformComponent::class).get()) {
    private var accumulator = 0f

    override fun update(deltaTime: Float) {
        if (world.autoClearForces) {
            LOG.error { "AutoClearForces must be set to false to guarantee a correct physic step behavior." }
            world.autoClearForces = false
        }

        accumulator += min(1 / 30f, deltaTime)
        while (accumulator >= physicTimeStep) {
            updatePrevPositionAndApplyForces()
            world.step(physicTimeStep, 6, 2)
            accumulator -= physicTimeStep
        }
        world.clearForces()

        updateInterpolatedPosition(accumulator / physicTimeStep)
    }

    private fun updatePrevPositionAndApplyForces() {
        entities.forEach { entity ->
            val transformCmp = entity.transformCmp
            val halfW = transformCmp.size.x * 0.5f
            val halfH = transformCmp.size.y * 0.5f
            val box2dCmp = entity.box2dCmp
            val body = box2dCmp.body

            transformCmp.position.set(
                body.position.x - halfW,
                body.position.y - halfH,
                transformCmp.position.z
            )

            // calculate impulse to apply
            entity[MoveComponent.MAPPER]?.let { moveCmp ->
                box2dCmp.impulse.x = body.mass * (moveCmp.speed * cosDeg(moveCmp.directionDeg) - body.linearVelocity.x)
                box2dCmp.impulse.y = body.mass * (moveCmp.speed * sinDeg(moveCmp.directionDeg) - body.linearVelocity.y)
            }

            if (!box2dCmp.impulse.isZero) {
                body.applyLinearImpulse(box2dCmp.impulse, body.worldCenter, true)
                box2dCmp.impulse.set(0f, 0f)
            }
        }
    }

    private fun updateInterpolatedPosition(alpha: Float) {
        entities.forEach { entity ->
            val transformCmp = entity.transformCmp
            val halfW = transformCmp.size.x * 0.5f
            val halfH = transformCmp.size.y * 0.5f
            val box2dCmp = entity.box2dCmp
            val body = box2dCmp.body

            // transform position contains the previous position of the body before world.step.
            // we use it for the interpolation for the render position
            box2dCmp.renderPosition.x = MathUtils.lerp(transformCmp.position.x, body.position.x - halfW, alpha)
            box2dCmp.renderPosition.y = MathUtils.lerp(transformCmp.position.y, body.position.y - halfH, alpha)

            // update transform position to correct body position
            transformCmp.position.set(
                body.position.x - halfW,
                body.position.y - halfH,
                transformCmp.position.z,
            )
        }
    }

    override fun processEntity(entity: Entity?, deltaTime: Float) = Unit

    companion object {
        private val LOG = logger<Box2DSystem>()
    }
}