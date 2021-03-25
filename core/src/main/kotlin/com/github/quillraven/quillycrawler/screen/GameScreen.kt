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
import com.github.quillraven.quillycrawler.ashley.configureTiledMapEntity
import com.github.quillraven.quillycrawler.ashley.system.*
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.SoundAssets
import ktx.ashley.EngineEntity
import ktx.log.debug
import ktx.log.logger

class GameScreen(
  private val game: QuillyCrawler,
  private val messageManager: MessageManager = MessageManager.getInstance()
) : AbstractScreen(game) {
  private val gameViewport = FitViewport(16f, 9f)
  private val world = World(Vector2.Zero, true).apply {
    autoClearForces = false
  }
  private val box2DDebugRenderer = Box2DDebugRenderer()
  private val mapService: MapService =
    TiledMapService(assetStorage, batch, QuillyCrawler.UNIT_SCALE, world, EngineEntity::configureTiledMapEntity)
  private val engine = PooledEngine().apply {
    addSystem(PlayerControlSystem())
    addSystem(InteractSystem(messageManager, game.audioService))
    addSystem(StateSystem(messageManager, MessageType.values().map { it.ordinal }.toSet()))
    addSystem(LootSystem())
    addSystem(GearSystem())
    addSystem(ConsumeSystem())
    addSystem(MoveSystem())
    addSystem(Box2DSystem(world, 1 / 60f))
    addSystem(CameraLockSystem(gameViewport.camera))
    addSystem(CollisionSystem(world))
    addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
    addSystem(RenderSystem(batch, gameViewport, mapService = mapService))
    if (game.isDevMode()) {
      addSystem(Box2DDebugRenderSystem(world, gameViewport, box2DDebugRenderer))
    }
    addSystem(MapSystem(mapService, game.audioService))
    addSystem(RemoveSystem())
    addSystem(SetScreenSystem(game))
  }

  override fun resize(width: Int, height: Int) {
    gameViewport.update(width, height, true)
  }

  override fun render(delta: Float) {
    // TODO remove debug sound stuff
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
      for (i in 0..5) {
        with(MusicAssets.values().random().descriptor.fileName) {
          game.audioService.playMusic(this)
          LOG.debug { "Playing music $this" }
        }
      }

      for (x in 0..2) {
        for (i in 0..50) {
          with(SoundAssets.values().random().descriptor.fileName) {
            game.audioService.playSound(this)
            LOG.debug { "Playing sound $this" }
          }
        }
        game.audioService.update()
      }
    } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
      game.audioService.playSound("wrongpath")
      game.audioService.update()
      game.audioService.playMusic("wrongpath")
    }

    engine.update(delta)
  }

  override fun dispose() {
    world.dispose()
    box2DDebugRenderer.dispose()
    LOG.debug { "'${engine.entities.size()}' entities in engine" }
  }

  companion object {
    private val LOG = logger<GameScreen>()
  }
}
