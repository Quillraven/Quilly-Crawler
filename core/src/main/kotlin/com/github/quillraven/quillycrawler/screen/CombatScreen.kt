package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.ashley.component.tiledCmp
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.system.*
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.command.CommandAttack
import com.github.quillraven.quillycrawler.combat.command.CommandProtect
import com.github.quillraven.quillycrawler.combat.configureEnemyCombatEntity
import com.github.quillraven.quillycrawler.combat.configurePlayerCombatEntity
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.allOf
import ktx.ashley.entity
import ktx.ashley.exclude
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.collections.set

class CombatScreen(
  private val game: QuillyCrawler,
  private val gameEngine: Engine,
  var playerEntity: Entity,
  var enemyEntity: Entity,
  private val gameEventDispatcher: GameEventDispatcher = game.gameEventDispatcher
) : AbstractScreen(game), GameEventListener {
  private val gameViewport = game.gameViewport
  private val engine = PooledEngine().apply {
    val combatContext = CombatContext(this, audioService)

    addSystem(CombatAiSystem())
    addSystem(CombatSystem(combatContext, gameEventDispatcher))
    addSystem(BuffSystem(combatContext, gameEventDispatcher))
    addSystem(ConsumeSystem())
    addSystem(DamageEmitterSystem(gameEventDispatcher))
    addSystem(FadeSystem())
    addSystem(AnimationSystem(game.assetStorage, QuillyCrawler.UNIT_SCALE))
    addSystem(ShakeSystem())
    addSystem(RenderSystem(game.batch, gameViewport))
    addSystem(RemoveSystem())
  }
  private var playerCombatEntity = playerEntity
  private var combatOver = false
  private val enemyEncounters = ObjectMap<String, GdxArray<String>>().apply {
    this["CHORT"] = gdxArrayOf("CHORT", "IMP")
    this["IMP"] = gdxArrayOf("CHORT", "IMP")
    this["SKELET"] = gdxArrayOf("SKELET", "GOBLIN")
  }
  private var gameMusic = ""
  private var playerTurn = false

  override fun show() {
    super.show()
    combatOver = false
    gameEventDispatcher.addListener(GameEventType.COMBAT_VICTORY, this)
    gameEventDispatcher.addListener(GameEventType.COMBAT_DEFEAT, this)
    gameEventDispatcher.addListener(GameEventType.PLAYER_TURN, this)
    gameMusic = audioService.currentMusicFilePath

    playerCombatEntity = engine.entity { configurePlayerCombatEntity(playerEntity, gameViewport) }

    spawnEnemies()
  }

  private fun spawnEnemies() {
    val dungeonLevel = playerEntity.playerCmp.dungeonLevel
    val tiledCmp = enemyEntity.tiledCmp
    when (val enemyType = tiledCmp.type) {
      "BOSS" -> {
        audioService.play(MusicAssets.LASER_QUEST)

        // spawn exact boss setup
        when (tiledCmp.name) {
          "BIG_DEMON" -> {
            val bossEnemies = gdxArrayOf("CHORT", "BIG_DEMON", "IMP")
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

  override fun hide() {
    super.hide()
    gameEventDispatcher.removeListener(this)
    audioService.playMusic(gameMusic)
    engine.removeAllEntities()
  }

  override fun onEvent(event: GameEvent) {
    when (event) {
      is CombatVictoryEvent -> {
        combatOver = true
        audioService.play(MusicAssets.VICTORY, loop = false)
        // remove enemy entity from original game screen
        enemyEntity.removeFromEngine(gameEngine, 1.5f)
        enemyEntity.fadeTo(gameEngine, 1f, 0f, 0f, 0f, 1.5f)
        playerEntity.interactCmp.entitiesInRange.remove(enemyEntity)
        updatePlayerItemsAfterCombat()
      }
      is CombatDefeatEvent -> {
        combatOver = true
        audioService.play(MusicAssets.DEFEAT, loop = false)
        updatePlayerItemsAfterCombat(true)
      }
      is CombatPlayerTurnEvent -> {
        playerTurn = true
      }
      else -> Unit
    }
  }

  private fun updatePlayerItemsAfterCombat(reduceGold: Boolean = false) {
    with(playerEntity.bagCmp) {
      items.clear()
      playerCombatEntity.bagCmp.items.forEach { entry -> items[entry.key] = entry.value }

      if (reduceGold) {
        gold = (gold * 0.8f).toInt()
      }
    }
  }

  override fun render(delta: Float) {
    //TODO remove debug stuff
    if (playerTurn && Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
      playerTurn = false

      val enemyEntities = engine.getEntitiesFor(
        allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get()
      )
      lateinit var aliveEntity: Entity
      enemyEntities.forEach { entity ->
        if (entity.isAlive) {
          aliveEntity = entity
          return@forEach
        }
      }
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach {
        it.combatCmp.availableCommands[CommandAttack::class].targets.add(aliveEntity)
        gameEventDispatcher.dispatchEvent<CombatCommandPlayer> {
          this.command = it.combatCmp.availableCommands[CommandAttack::class]
        }
      }
    } else if (playerTurn && Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
      playerTurn = false
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach {
        gameEventDispatcher.dispatchEvent<CombatCommandPlayer> {
          this.command = it.combatCmp.availableCommands[CommandProtect::class]
        }
      }
    } else if (combatOver && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
      game.setScreen<GameScreen>()
    } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      game.setScreen<GameScreen>()
    }

    engine.update(delta)
  }
}
