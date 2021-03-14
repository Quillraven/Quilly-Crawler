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
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.configureTiledMapEntity
import com.github.quillraven.quillycrawler.ashley.system.*
import ktx.ashley.EngineEntity
import ktx.ashley.allOf
import ktx.ashley.configureEntity
import ktx.ashley.with
import ktx.collections.isNotEmpty
import ktx.log.debug
import ktx.log.logger

class PlayGroundScreen(
  private val game: QuillyCrawler,
  private val messageManager: MessageManager = MessageManager.getInstance()
) : AbstractScreen(game) {
  private val gameViewport = FitViewport(16f, 9f)
  private val world = World(Vector2.Zero, true).apply {
    autoClearForces = false
  }
  private val box2DDebugRenderer = Box2DDebugRenderer()
  private val engine = PooledEngine()
  private val mapService: MapService =
    TiledMapService(engine, assetStorage, batch, QuillyCrawler.UNIT_SCALE, EngineEntity::configureTiledMapEntity, world)

  init {
    // systems must be added before any entity is added to the engine because some systems react on
    // entity creation with certain components for initialization
    engine.run {
      addSystem(PlayerControlSystem(game))
      addSystem(InteractSystem(messageManager))
      addSystem(StateSystem(messageManager, MessageType.values().map { it.ordinal }.toSet()))
      addSystem(LootSystem())
      addSystem(GearSystem())
      addSystem(MoveSystem())
      addSystem(Box2DSystem(world, 1 / 60f))
      addSystem(CameraLockSystem(gameViewport.camera))
      addSystem(CollisionSystem(world))
      addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
      addSystem(RenderSystem(batch, gameViewport, mapService = mapService))
      if (game.isDevMode()) {
        addSystem(Box2DDebugRenderSystem(world, gameViewport, box2DDebugRenderer))
      }
      addSystem(MapSystem(mapService))
      addSystem(RemoveSystem())
    }
  }

  override fun resize(width: Int, height: Int) {
    gameViewport.update(width, height, true)
  }

  override fun render(delta: Float) {
    // TODO remove debug stuff
    when {
      Gdx.input.isKeyJustPressed(Input.Keys.E) -> {
        engine.getEntitiesFor(allOf(PlayerComponent::class, BagComponent::class, GearComponent::class).get()).forEach {
          val bagCmp = it.bagCmp

          if (bagCmp.items.isNotEmpty()) {
            engine.configureEntity(it) {
              with<EquipComponent> {
                addToGear.add(bagCmp.items.first().value)
              }
            }
          }
        }
      }
      Gdx.input.isKeyJustPressed(Input.Keys.U) -> {
        engine.getEntitiesFor(allOf(PlayerComponent::class, BagComponent::class, GearComponent::class).get()).forEach {
          val bagCmp = it.bagCmp

          if (bagCmp.items.isNotEmpty()) {
            engine.configureEntity(it) {
              with<EquipComponent> {
                removeFromGear.add(bagCmp.items.first().value)
              }
            }
          }
        }
      }
      Gdx.input.isKeyJustPressed(Input.Keys.P) -> {
        engine.getEntitiesFor(allOf(PlayerComponent::class, BagComponent::class, GearComponent::class).get()).forEach {
          with(it.statsCmp) {
            StatsType.values().forEach { statsType ->
              LOG.debug { "${statsType.name} -> ${this.totalStatValue(it, statsType)}" }
            }
          }
        }
      }
    }

    engine.update(delta)
  }

  override fun dispose() {
    world.dispose()
    box2DDebugRenderer.dispose()
    LOG.debug { "'${engine.entities.size()}' entities in engine" }
  }

  companion object {
    private val LOG = logger<PlayGroundScreen>()
  }
}
