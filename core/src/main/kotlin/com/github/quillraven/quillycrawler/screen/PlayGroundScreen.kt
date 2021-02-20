package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.component.PlayerComponent
import com.github.quillraven.commons.ashley.component.box2dCmp
import com.github.quillraven.commons.ashley.component.transformCmp
import com.github.quillraven.commons.ashley.entityByCfg
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.EntityType
import com.github.quillraven.quillycrawler.ashley.component.CollectableComponent
import com.github.quillraven.quillycrawler.ashley.component.CollectingComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import com.github.quillraven.quillycrawler.ashley.system.CollisionSystem
import com.github.quillraven.quillycrawler.ashley.system.MoveSystem
import com.github.quillraven.quillycrawler.ashley.system.PlayerControlSystem
import ktx.ashley.with
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

            entityByCfg(3f, 3f, EntityType.CHEST.name, game.entityConfigurations, world) {
                with<CollectableComponent>()
            }

            entityByCfg(1f, 0f, EntityType.BIG_DEMON.name, game.entityConfigurations)
        }
    }

    private fun PooledEngine.playerEntity() {
        entityByCfg(0f, 0f, EntityType.PLAYER.name, game.entityConfigurations, world) {
            with<PlayerComponent>()
            with<PlayerControlComponent>()
            with<CollectingComponent>()
            this.entity.box2dCmp.body.circle(this.entity.transformCmp.size.x) {
                isSensor = true
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