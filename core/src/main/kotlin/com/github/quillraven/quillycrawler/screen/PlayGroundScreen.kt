package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.commons.map.TiledMapService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.newEntityFactory
import com.github.quillraven.quillycrawler.ashley.system.CollisionSystem
import com.github.quillraven.quillycrawler.ashley.system.MoveSystem
import com.github.quillraven.quillycrawler.ashley.system.PlayerControlSystem

class PlayGroundScreen(
    private val game: QuillyCrawler,
    private val messageManager: MessageManager = MessageManager.getInstance()
) : AbstractScreen(game) {
    private val viewport = FitViewport(16f, 9f)
    private val world = World(Vector2.Zero, true).apply {
        autoClearForces = false
    }
    private val box2DDebugRenderer = Box2DDebugRenderer()
    private val engine = PooledEngine()
    private val entityFactory = newEntityFactory(engine, world)
    private val mapService: MapService =
        TiledMapService(entityFactory, assetStorage, batch, QuillyCrawler.UNIT_SCALE)

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun show() {
        super.show()

        if (engine.systems.size() <= 0) {
            engine.run {
                addSystem(PlayerControlSystem(messageManager))
                addSystem(StateSystem(messageManager, MessageType.values().map { it.ordinal }.toSet()))
                addSystem(MoveSystem())
                addSystem(Box2DSystem(world, 1 / 60f))
                addSystem(CameraLockSystem(viewport.camera))
                addSystem(CollisionSystem(world))
                addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
                addSystem(RenderSystem(batch, viewport, mapService = mapService))
                if (game.isDevMode()) {
                    addSystem(Box2DDebugRenderSystem(world, viewport, box2DDebugRenderer))
                }
            }
        }

        mapService.setMap(engine, "maps/tutorial.tmx")
    }

    override fun hide() {
        super.hide()
        engine.removeAllEntities()
    }

    override fun render(delta: Float) {
        //TODO remove debug stuff
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            mapService.setMap(engine, "maps/test1.tmx")
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            mapService.setMap(engine, "maps/test2.tmx")
        }

        engine.update(delta)
    }

    override fun dispose() {
        world.dispose()
        box2DDebugRenderer.dispose()
    }
}