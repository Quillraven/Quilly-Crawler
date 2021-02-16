package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageManager
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
import com.github.quillraven.quillycrawler.ai.ChestState
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ai.PlayerState
import com.github.quillraven.quillycrawler.ashley.component.CollectableComponent
import com.github.quillraven.quillycrawler.ashley.component.CollectingComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import com.github.quillraven.quillycrawler.ashley.system.CollisionSystem
import com.github.quillraven.quillycrawler.ashley.system.PlayerControlSystem
import ktx.ashley.entity
import ktx.ashley.with
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.circle

class PlayGroundScreen(
    private val game: QuillyCrawler,
    private val messageManager: MessageManager = MessageManager.getInstance()
) : AbstractScreen(game) {
    private val viewport = FitViewport(16f, 9f)
    private val world = World(Vector2.Zero, true).apply {
        autoClearForces = false
    }
    private val box2DDebugRenderer = Box2DDebugRenderer()
    private val engine = PooledEngine().apply {
        addSystem(PlayerControlSystem(messageManager))
        addSystem(EntityTypeStateAnimationSystem())
        addSystem(StateSystem(messageManager, MessageType.values().map { it.ordinal }.toSet()))
        addSystem(MoveSystem())
        addSystem(Box2DSystem(world, 1 / 60f))
        addSystem(CollisionSystem(world))
        addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
        addSystem(RenderSystem(batch, viewport))
        if (game.isDevMode()) {
            addSystem(Box2DDebugRenderSystem(world, viewport, box2DDebugRenderer))
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun show() {
        super.show()
        engine.run {
            playerEntity()

            entity {
                with<EntityTypeComponent> {
                    type = EntityType.CHEST
                }
                with<CollectableComponent>()
                val transformCmp = with<TransformComponent> {
                    position.set(3f, 3f, 0f)
                }
                with<AnimationComponent>()
                with<RenderComponent>()
                with<StateComponent> {
                    state = ChestState.IDLE
                }
                with<Box2DComponent> {
                    body = world.body(BodyDef.BodyType.StaticBody) {
                        position.set(
                            transformCmp.position.x + transformCmp.size.x * 0.5f,
                            transformCmp.position.y + transformCmp.size.y * 0.5f
                        )
                        fixedRotation = true
                        allowSleep = false
                        box(transformCmp.size.x, transformCmp.size.y) {
                            friction = 0f
                        }

                        userData = this@entity.entity
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

    private fun PooledEngine.playerEntity() {
        entity {
            with<PlayerComponent>()
            with<PlayerControlComponent>()
            with<CollectingComponent>()
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
                    val boundingBoxHeight = transformCmp.size.y * 0.2f
                    box(
                        transformCmp.size.x,
                        boundingBoxHeight,
                        Vector2(0f, -transformCmp.size.y * 0.5f + boundingBoxHeight * 0.5f)
                    ) {
                        friction = 0f
                    }
                    circle(transformCmp.size.x) {
                        isSensor = true
                    }

                    userData = this@entity.entity
                }
            }
        }
    }

    override fun hide() {
        super.hide()
        engine.removeAllEntities()
    }

    override fun render(delta: Float) {
        engine.update(delta)
    }

    override fun dispose() {
        world.dispose()
        box2DDebugRenderer.dispose()
    }
}