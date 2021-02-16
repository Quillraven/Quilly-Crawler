package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.commons.ashley.component.Box2DComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.box2dCmp
import com.github.quillraven.commons.ashley.component.transformCmp
import ktx.ashley.allOf
import ktx.log.error
import ktx.log.logger
import kotlin.math.min

/**
 * System to update the [world] using a fixed timestep implementation. The rate of the update calls is defined
 * by [physicTimeStep].
 * To avoid the spiral of death a fixed maximum time of 1/30 seconds between two frames is used.
 *
 * Make sure to set the [world's][World] autoClearForces to false. Otherwise, the system will automatically set it
 * and log an error. Forces get cleared after all [World.step] calls are done.
 *
 * Applies the [Box2DComponent.impulse] to a [Body] before a call to [World.step].
 * The impulse gets set to zero afterwards.
 *
 * The system also updates the [Box2DComponent.renderPosition] by using an interpolation between the position of a
 * [Body] before [World.step] and after. Use this position for a smoother render experience.
 * In case of a box body the position represents the bottom left corner. It is calculated by using the [body's][Body]
 * position, which is usually the center, and the [TransformComponent.size].
 *
 * [TransformComponent.position] is linked to the [Body.position]. It also represents the bottom left corner
 * like the [Box2DComponent.renderPosition].
 */
class Box2DSystem(
    private val world: World,
    private val physicTimeStep: Float
) : IteratingSystem(allOf(Box2DComponent::class, TransformComponent::class).get()) {
    private var accumulator = 0f

    /**
     * Updates the [world] using a fixed timestep of [physicTimeStep]
     */
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

        updatePositionAndRenderPosition(accumulator / physicTimeStep)
    }

    /**
     * Applies an impulse once to all entities and updates their [TransformComponent.position] to the position
     * before calling [World.step].
     */
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

            if (!box2dCmp.impulse.isZero) {
                // apply non-zero impulse once before a call to world.step
                body.applyLinearImpulse(box2dCmp.impulse, body.worldCenter, true)
                box2dCmp.impulse.set(0f, 0f)
            }
        }
    }

    /**
     * Updates the [TransformComponent.position] and [Box2DComponent.renderPosition] of all entities.
     */
    private fun updatePositionAndRenderPosition(alpha: Float) {
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