package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.EntityType
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.BigDemonState
import com.github.quillraven.quillycrawler.ai.PlayerState
import ktx.ashley.addComponent
import ktx.ashley.entity
import ktx.ashley.with
import ktx.box2d.body
import ktx.box2d.box

class PlayGroundScreen(
    private val game: QuillyCrawler
) : AbstractScreen(game) {
    private val viewport = FitViewport(16f, 9f)
    private val world = World(Vector2.Zero, true).apply {
        autoClearForces = false
    }
    private val box2DDebugRenderer = Box2DDebugRenderer()
    private val engine = PooledEngine().apply {
        addSystem(EntityTypeStateAnimationSystem())
        addSystem(StateSystem())
        addSystem(MoveSystem())
        addSystem(Box2DSystem(world, 1 / 60f))
        addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
        addSystem(RenderSystem(batch, viewport))
        if (game.isDevMode()) {
            addSystem(Box2DDebugRenderSystem(world, viewport, box2DDebugRenderer))
        }
    }

    override fun inputProcessor(): InputProcessor {
        return game.inputServiceProvider.inputService
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun show() {
        super.show()
        engine.run {
            entity {
                with<EntityTypeComponent> {
                    type = EntityType.WIZARD_MALE
                }
                val transformCmp = with<TransformComponent>()
                with<AnimationComponent>()
                with<RenderComponent>()
                with<StateComponent> {
                    state = PlayerState.IDLE
                }
                with<Box2DComponent> {
                    body = world.body(BodyDef.BodyType.DynamicBody) {
                        position.set(
                            transformCmp.position.x + transformCmp.size.x * 0.5f,
                            transformCmp.position.y + transformCmp.size.y * 0.5f
                        )
                        fixedRotation = true
                        allowSleep = false
                        box(transformCmp.size.x, transformCmp.size.y) {
                            friction = 0f
                        }
                    }
                }
            }

            entity {
                with<EntityTypeComponent> {
                    type = EntityType.BIG_DEMON
                }
                with<TransformComponent> {
                    position.x = 1f
                    size.x = 0.75f
                    size.y = 0.75f
                }
                with<AnimationComponent>()
                with<RenderComponent>()
                with<StateComponent> {
                    state = BigDemonState.RUN
                }
            }
        }
    }

    override fun hide() {
        super.hide()
        engine.removeAllEntities()
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            engine.entities.first().addComponent<MoveComponent>(engine) {
                directionDeg = 0f
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            engine.entities.first().addComponent<MoveComponent>(engine) {
                directionDeg = 180f
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            engine.entities.first().addComponent<MoveComponent>(engine) {
                directionDeg = 90f
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            engine.entities.first().addComponent<MoveComponent>(engine) {
                directionDeg = 270f
            }
        }

        engine.update(delta)
    }

    override fun dispose() {
        world.dispose()
        box2DDebugRenderer.dispose()
    }
}