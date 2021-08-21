package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.github.quillraven.commons.ashley.component.Box2DComponent
import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.ashley.component.tiledCmp
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.system.*
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.configureEnemyCombatEntity
import com.github.quillraven.quillycrawler.combat.configurePlayerCombatEntity
import com.github.quillraven.quillycrawler.event.*
import com.github.quillraven.quillycrawler.ui.model.CombatState
import com.github.quillraven.quillycrawler.ui.model.CombatViewModel
import com.github.quillraven.quillycrawler.ui.view.CombatView
import ktx.ashley.configureEntity
import ktx.ashley.entity
import ktx.ashley.getSystem
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.collections.set

class CombatScreen(
  private val game: QuillyCrawler,
  private val gameEngine: Engine,
  var playerEntity: Entity,
  var enemyEntity: Entity,
  private val gameEventDispatcher: GameEventDispatcher = game.gameEventDispatcher
) : AbstractScreen(game) {
  private val gameViewport = game.gameViewport
  private val engine = PooledEngine().apply {
    val combatContext = CombatContext(this, audioService, gameEventDispatcher)

    addSystem(FadeSystem())
    addSystem(ResizeSystem())
    addSystem(AnimationSystem(game.assetStorage, QuillyCrawler.UNIT_SCALE))
    addSystem(ShakeSystem())
    addSystem(RenderSystem(game.batch, gameViewport))
    addSystem(RemoveSystem())
    // It is important that AnimationSystem and render stuff runs BEFORE the real logic
    // because the UI requires that the sprites for the combat entities are already initialized.
    // Otherwise the order entity table will not show anything in the UI because sprite's region size is 0/0
    addSystem(CombatAiSystem())
    addSystem(CombatSystem(combatContext, gameEventDispatcher))
    addSystem(BuffSystem(combatContext, gameEventDispatcher))
    addSystem(ConsumeSystem(gameEventDispatcher))
    addSystem(HealEmitterSystem(gameEventDispatcher))
    addSystem(DamageEmitterSystem(gameEventDispatcher))
    if (game.renderDebug()) {
      addSystem(DebugBoundingAreaSystem(assetStorage, gameViewport))
    }
  }
  private var playerCombatEntity = playerEntity
  private var gameMusic = ""
  private val enemyEncounters = ObjectMap<String, GdxArray<String>>().apply {
    this["CHORT"] = gdxArrayOf("CHORT", "IMP")
    this["IMP"] = gdxArrayOf("CHORT", "IMP")
    this["SKELET"] = gdxArrayOf("SKELET", "GOBLIN")
  }
  private val viewModel =
    CombatViewModel(assetStorage[I18NAssets.DEFAULT.descriptor], engine, game, ::onReturnToGame)
  private val view = CombatView(viewModel)
  private var gameFbo = FrameBuffer(Pixmap.Format.RGB888, Gdx.graphics.width, Gdx.graphics.height, false)
  private var gameTransitionAlpha = 0f
  private val gameColor = Color(1f, 1f, 1f, 1f)
  private var blurRadius = 0f
  private var returnToGame = false

  override fun show() {
    super.show()
    returnToGame = false

    // remember previous game music
    gameMusic = audioService.currentMusicFilePath

    // spawn combat entities
    engine.getSystem<CombatSystem>().cleanupTurn(true)
    playerCombatEntity = engine.entity { configurePlayerCombatEntity(playerEntity, gameViewport) }
    spawnEnemies()

    // setup UI stuff
    gameColor.a = 1f
    blurRadius = 0f
    gameTransitionAlpha = 0f

    viewModel.combatState = CombatState.RUNNING
    with(gameEventDispatcher) {
      addListener<CombatVictoryEvent>(viewModel)
      addListener<CombatDefeatEvent>(viewModel)
      addListener<CombatNewTurnEvent>(viewModel)
      addListener<CombatStartEvent>(viewModel)
      addListener<CombatPostDamageEvent>(viewModel)
      addListener<CombatPostHealEvent>(viewModel)
      addListener<CombatCommandStarted>(viewModel)
      addListener<CombatConsumeItemEvent>(viewModel)
      addListener<CombatBuffAdded>(viewModel)
      addListener<CombatBuffRemoved>(viewModel)
    }
    stage.addActor(view)
  }

  override fun hide() {
    super.hide()

    if (viewModel.combatState == CombatState.VICTORY) {
      // remove enemy entity from original game screen
      enemyEntity.removeFromEngine(gameEngine, 1.5f)
      enemyEntity.fadeTo(gameEngine, 1f, 0f, 0f, 0f, 1.5f)
      enemyEntity.remove(Box2DComponent::class.java)
      playerEntity.interactCmp.entitiesInRange.remove(enemyEntity)
      gameEngine.configureEntity(playerEntity) {
        with<CombatLootComponent> {
          victory = true
          gold = 10 + playerEntity.playerCmp.dungeonLevel * MathUtils.random(1, 3)
        }
      }
    } else {
      // defeat -> reduce gold
      gameEngine.configureEntity(playerEntity) {
        with<CombatLootComponent> {
          victory = false
          gold = -(playerEntity.bagCmp.gold * 0.15f).toInt()
        }
      }
    }

    gameEventDispatcher.removeListener(viewModel)
  }

  private fun spawnEnemies() {
    val dungeonLevel = playerEntity.playerCmp.dungeonLevel
    val tiledCmp = enemyEntity.tiledCmp
    when (val enemyType = tiledCmp.type) {
      "BOSS" -> {
        // spawn exact boss setup
        when (tiledCmp.name) {
          "BIG_DEMON" -> {
            audioService.play(MusicAssets.LASER_QUEST)
            val bossEnemies = gdxArrayOf("CHORT", "BIG_DEMON", "IMP")
            bossEnemies.forEachIndexed { index, name ->
              engine.entity { configureEnemyCombatEntity(name, dungeonLevel, gameViewport, index, bossEnemies.size) }
            }
          }
          "GOBLIN" -> {
            audioService.play(MusicAssets.QUANTUM_LOOP)
            val bossEnemies = gdxArrayOf("GOBLIN")
            bossEnemies.forEachIndexed { index, name ->
              engine.entity { configureEnemyCombatEntity(name, dungeonLevel, gameViewport, index, bossEnemies.size) }
            }
          }
          else -> {
            throw GdxRuntimeException("Unsupported boss $enemyType")
          }
        }
      }
      else -> {
        audioService.play(MusicAssets.QUANTUM_LOOP)

        // spawn between 1 to 4 enemies depending on difficulty
        val numEnemies = when (enemyType) {
          "EASY" -> MathUtils.random(1, 2)
          "MEDIUM" -> MathUtils.random(2, 3)
          "HARD" -> MathUtils.random(3, 4)
          else -> 1
        }

        // create enemy of exact type that you see on the map
        engine.entity { configureEnemyCombatEntity(tiledCmp.name, dungeonLevel, gameViewport, 0, numEnemies) }
        // create remaining enemies which can be of a different type
        for (i in 1 until numEnemies) {
          engine.entity {
            configureEnemyCombatEntity(
              enemyType(tiledCmp.name),
              dungeonLevel,
              gameViewport,
              i,
              numEnemies
            )
          }
        }
      }
    }
  }

  private fun enemyType(enemyName: String): String {
    val enemyTypes = enemyEncounters[enemyName]
    return if (enemyTypes == null) {
      enemyName
    } else {
      enemyTypes.random()
    }
  }

  private fun updatePlayerAfterCombat() {
    // update items
    with(playerEntity.bagCmp) {
      items.clear()
      playerCombatEntity.bagCmp.items.forEach { entry -> items[entry.key] = entry.value }
    }

    // update commands if any command was learned during combat
    with(playerEntity.combatCmp) {
      playerCombatEntity.combatCmp.availableCommands.forEach {
        if (!this.commandsToLearn.contains(it.key)) {
          this.learn(it.key)
        }
      }
    }

    // update stats
    if (viewModel.combatState == CombatState.VICTORY) {
      // victory -> set stats to combat stats
      with(playerEntity.statsCmp) {
        StatsType.VALUES.forEach { this[it] = playerCombatEntity.statsCmp[it] }
      }
    } else {
      // defeat -> restore life and mana to maximum
      with(playerEntity.statsCmp) {
        StatsType.VALUES.forEach {
          when (it) {
            StatsType.LIFE -> this[StatsType.LIFE] = this.totalStatValue(playerEntity, StatsType.MAX_LIFE)
            StatsType.MANA -> this[StatsType.MANA] = this.totalStatValue(playerEntity, StatsType.MAX_MANA)
            else -> this[it] = playerCombatEntity.statsCmp[it]
          }
        }
      }
    }
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    if (gameFbo.width != width || gameFbo.height != height) {
      gameFbo.dispose()
      gameFbo = FrameBuffer(Pixmap.Format.RGB888, width, height, false)
    }

    // resize is called when the screen gets active or the resolution changes
    // -> render GameScreen scene to a separate fbo to render it as a background for the combat
    gameFbo.bind()
    ScreenUtils.clear(0f, 0f, 0f, 0f, false)
    HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    gameEngine.getSystem<RenderSystem>().update(0f)
    FrameBuffer.unbind()
  }

  // gets called by ViewModel when combat is over (=victory or defeat)
  private fun onReturnToGame() {
    returnToGame = true
    gameTransitionAlpha = 0f
    stage.clear()

    // return to previous game music
    audioService.playMusic(gameMusic)

    // update player entity after combat
    updatePlayerAfterCombat()

    // cleanup combat engine
    engine.removeAllEntities()
  }

  override fun render(delta: Float) {
    if (gameTransitionAlpha < 1f) {
      gameTransitionAlpha = (gameTransitionAlpha + delta).coerceAtMost(1f)
      if (returnToGame) {
        blurRadius = MathUtils.lerp(TARGET_BLUR_RADIUS, 0f, gameTransitionAlpha)
        gameColor.a = MathUtils.lerp(TARGET_ALPHA, 1f, gameTransitionAlpha)
        if (gameTransitionAlpha >= 1f) {
          game.setScreen<GameScreen>()
        }
      } else {
        blurRadius = MathUtils.lerp(0f, TARGET_BLUR_RADIUS, gameTransitionAlpha)
        gameColor.a = MathUtils.lerp(1f, TARGET_ALPHA, gameTransitionAlpha)
      }
    }

    game.shaderService.renderTextureBlurred(gameFbo.colorBufferTexture, blurRadius, gameColor)
    engine.update(delta)
  }

  override fun dispose() {
    gameFbo.dispose()
  }

  companion object {
    private const val TARGET_BLUR_RADIUS = 6f
    private const val TARGET_ALPHA = 0.7f
  }
}
