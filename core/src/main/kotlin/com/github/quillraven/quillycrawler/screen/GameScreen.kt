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
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.configureTiledMapEntity
import com.github.quillraven.quillycrawler.ashley.system.*
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.event.*
import com.github.quillraven.quillycrawler.preferences.loadGameState
import com.github.quillraven.quillycrawler.preferences.saveGameState
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
  private val viewModel = GameViewModel(assetStorage[I18NAssets.DEFAULT.descriptor], engine, audioService)
  private val view = GameView(viewModel)
  private var loadSaveState = false

  init {
    engine.run {
      addSystem(PlayerControlSystem(gameEventDispatcher))
      addSystem(InteractSystem(messageManager, audioService, gameEventDispatcher))
      addSystem(StateSystem(messageManager, MessageType.values().map { it.ordinal }.toSet()))
      addSystem(LootSystem(gameEventDispatcher))
      addSystem(CombatLootSystem(gameEventDispatcher))
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
          shaderService = game.shaderService
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
    if (game.shaderService is DefaultShaderService) {
      (game.shaderService as DefaultShaderService).activeEngine = engine
    }
    gameEventDispatcher.addListener<MapChangeEvent>(viewModel)
    gameEventDispatcher.addListener<GameInteractReaperEvent>(viewModel)
    gameEventDispatcher.addListener<GameExitEvent>(viewModel)
    gameEventDispatcher.addListener<GameLootEvent>(viewModel)
    gameEventDispatcher.addListener<GameCombatLoot>(viewModel)
    stage.addActor(view)
    engine.getSystem<AmbientSoundSystem>().setProcessing(true)
    engine.getSystem<PlayerControlSystem>().setProcessing(true)
  }

  override fun hide() {
    super.hide()
    if (game.shaderService is DefaultShaderService) {
      (game.shaderService as DefaultShaderService).activeEngine = null
    }
    gameEventDispatcher.removeListener(viewModel)
    engine.getSystem<AmbientSoundSystem>().setProcessing(false)
    engine.getSystem<PlayerControlSystem>().setProcessing(false)
  }

  fun loadSaveState() {
    loadSaveState = true
  }

  override fun render(delta: Float) {
    engine.update(delta)

    if (loadSaveState) {
      // load state in render loop after engine.update is called
      // otherwise the MapSystem and other things are not correctly initialized
      loadSaveState = false
      game.preferences.loadGameState(engine)
    }
  }

  override fun dispose() {
    game.preferences.saveGameState(engine)
    world.dispose()
    LOG.debug { "'${engine.entities.size()}' entities in engine" }
  }

  companion object {
    private val LOG = logger<GameScreen>()
  }
}
