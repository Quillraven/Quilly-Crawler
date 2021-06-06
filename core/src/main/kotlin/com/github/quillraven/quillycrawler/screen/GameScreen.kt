package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.commons.map.TiledMapService
import com.github.quillraven.commons.shader.ShaderService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.configureTiledMapEntity
import com.github.quillraven.quillycrawler.ashley.system.*
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.MapChangeEvent
import com.github.quillraven.quillycrawler.shader.DefaultShaderService
import com.github.quillraven.quillycrawler.ui.model.GameViewModel
import com.github.quillraven.quillycrawler.ui.view.GameView
import kotlinx.coroutines.launch
import ktx.ashley.EngineEntity
import ktx.ashley.getSystem
import ktx.async.KtxAsync
import ktx.log.debug
import ktx.log.logger

class GameScreen(
  private val game: QuillyCrawler,
  private val messageManager: MessageManager = MessageManager.getInstance(),
  private val gameEventDispatcher: GameEventDispatcher = game.gameEventDispatcher
) : AbstractScreen(game) {

  private val world = World(Vector2.Zero, true).apply {
    autoClearForces = false
  }
  private val mapService: MapService =
    TiledMapService(
      assetStorage,
      batch,
      QuillyCrawler.UNIT_SCALE,
      world,
      audioService,
      EngineEntity::configureTiledMapEntity
    )
  private val engine = PooledEngine()
  private val shaderService: ShaderService = DefaultShaderService(assetStorage, batch, engine)
  private val viewModel = GameViewModel(assetStorage[I18NAssets.DEFAULT.descriptor])
  private val view = GameView(viewModel)

  init {
    engine.run {
      addSystem(PlayerControlSystem())
      addSystem(InteractSystem(messageManager, audioService))
      addSystem(StateSystem(messageManager, MessageType.values().map { it.ordinal }.toSet()))
      addSystem(LootSystem())
      addSystem(GearSystem())
      addSystem(ConsumeSystem(gameEventDispatcher))
      addSystem(MoveSystem())
      addSystem(Box2DSystem(world, 1 / 60f))
      addSystem(CameraLockSystem(game.gameViewport.camera))
      addSystem(CollisionSystem(world))
      addSystem(FadeSystem())
      addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
      addSystem(OutlineColorSystem())
      addSystem(
        RenderSystem(
          batch,
          game.gameViewport,
          mapService = mapService,
          shaderService = shaderService
        )
      )
      if (game.b2dDebug()) {
        val box2DDebugRenderer = Box2DDebugRenderer()
        addSystem(Box2DDebugRenderSystem(world, game.gameViewport, box2DDebugRenderer))
        KtxAsync.launch {
          assetStorage.add("b2dDebugRenderer", box2DDebugRenderer)
        }
      }
      addSystem(MapSystem(mapService, gameEventDispatcher))
      addSystem(AmbientSoundSystem(audioService))
      addSystem(RemoveSystem())
      addSystem(SetScreenSystem(game))
    }
  }

  override fun show() {
    super.show()
    gameEventDispatcher.addListener<MapChangeEvent>(viewModel)
    stage.addActor(view)
    engine.getSystem<AmbientSoundSystem>().setProcessing(true)
    engine.getSystem<PlayerControlSystem>().setProcessing(true)
  }

  override fun hide() {
    super.hide()
    gameEventDispatcher.removeListener(viewModel)
    engine.getSystem<AmbientSoundSystem>().setProcessing(false)
    engine.getSystem<PlayerControlSystem>().setProcessing(false)
  }

  override fun render(delta: Float) {
    engine.update(delta)
  }

  override fun dispose() {
    world.dispose()
    LOG.debug { "'${engine.entities.size()}' entities in engine" }
  }

  companion object {
    private val LOG = logger<GameScreen>()
  }
}
