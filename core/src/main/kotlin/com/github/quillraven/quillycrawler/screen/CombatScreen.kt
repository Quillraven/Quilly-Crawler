package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.fadeTo
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.system.BuffSystem
import com.github.quillraven.quillycrawler.ashley.system.CombatSystem
import com.github.quillraven.quillycrawler.ashley.system.ConsumeSystem
import com.github.quillraven.quillycrawler.ashley.system.DamageEmitterSystem
import com.github.quillraven.quillycrawler.ashley.withAnimationComponents
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.combat.command.CommandAttack
import com.github.quillraven.quillycrawler.combat.command.CommandProtect
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.allOf
import ktx.ashley.entity
import ktx.ashley.exclude
import ktx.ashley.with
import ktx.collections.isNotEmpty
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

  override fun show() {
    super.show()
    combatOver = false
    gameEventDispatcher.addListener(GameEventType.COMBAT_VICTORY, this)
    gameEventDispatcher.addListener(GameEventType.COMBAT_DEFEAT, this)
    gameEventDispatcher.addListener(GameEventType.PLAYER_TURN, this)
    audioService.play(MusicAssets.QUANTUM_LOOP)
    createPlayerCombatEntity(playerEntity)
    createEnemyCombatEntities(enemyEntity, playerEntity.playerCmp.dungeonLevel)
  }

  override fun hide() {
    super.hide()
    gameEventDispatcher.removeListener(this)
    audioService.playPreviousMusic()
    engine.removeAllEntities()
  }

  private fun createPlayerCombatEntity(playerEntity: Entity) {
    playerCombatEntity = engine.entity {
      with<TransformComponent> {
        position.set(
          gameViewport.camera.position.x - gameViewport.camera.viewportWidth * 0.5f + 6f,
          gameViewport.camera.position.y - gameViewport.camera.viewportHeight * 0.5f + 0.5f,
          position.z
        )
        size.set(1.5f, 1.5f)
      }
      withAnimationComponents(TextureAtlasAssets.ENTITIES, "wizard-m", "idle", 0f)
      with<PlayerComponent> { dungeonLevel = playerEntity.playerCmp.dungeonLevel }
      with<BagComponent> { playerEntity.bagCmp.items.forEach { entry -> items[entry.key] = entry.value } }
      with<GearComponent> { playerEntity.gearCmp.gear.forEach { entry -> gear[entry.key] = entry.value } }
      with<StatsComponent> { playerEntity.statsCmp.stats.forEach { entry -> stats[entry.key] = entry.value } }
      with<CombatComponent>()
      with<BuffComponent>()
    }
  }

  private fun createEnemyCombatEntities(enemyEntity: Entity, dungeonLevel: Int) {
    //TODO adjust combat stats by dungeonLevel
    //TODO get enemy strength information (EASY,MEDIUM,HARD) --> maybe part of Tiled map information?
    //TODO create 1-4 enemies if it is a NORMAL combat
    //TODO create exact boss setup if it is a BOSS combat
    engine.entity {
      with<TransformComponent> {
        position.set(
          gameViewport.camera.position.x - gameViewport.camera.viewportWidth * 0.5f + 8f,
          gameViewport.camera.position.y + gameViewport.camera.viewportHeight * 0.5f - 2.5f,
          position.z
        )
      }
      withAnimationComponents(TextureAtlasAssets.ENTITIES, "big-demon", "idle", 0f)
      with<StatsComponent> {
        stats[StatsType.AGILITY] = 1f
        stats[StatsType.LIFE] = 150f
        stats[StatsType.PHYSICAL_DAMAGE] = 1f
      }
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with<CombatComponent>()
      with<BuffComponent>()
    }
  }

  override fun onEvent(event: GameEvent) {
    when (event) {
      is CombatVictoryEvent -> {
        combatOver = true
        audioService.play(MusicAssets.VICTORY, loop = false)
        enemyEntity.removeFromEngine(gameEngine, 1.5f)
        enemyEntity.fadeTo(gameEngine, 1f, 0f, 0f, 0f, 1.5f)
        playerEntity.interactCmp.entitiesInRange.remove(enemyEntity)
        updatePlayerItemsAfterCombat()
      }
      is CombatDefeatEvent -> {
        combatOver = true
        audioService.play(MusicAssets.DEFEAT, loop = false)
        playerCombatEntity.fadeTo(engine, 1f, 0f, 0f, 0.5f, 1f)
        updatePlayerItemsAfterCombat(true)
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
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach {
        if (it.combatCmp.commandRequests.isNotEmpty()) return@forEach
        it.addCommandRequest(
          CommandAttack::class,
          engine.getEntitiesFor(allOf(CombatComponent::class).exclude(PlayerComponent::class).get()).random()
        )
      }
    } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach {
        if (it.combatCmp.commandRequests.isNotEmpty()) return@forEach
        it.addCommandRequest(CommandProtect::class)
      }
    } else if (combatOver && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
      game.setScreen<GameScreen>()
    }

    engine.update(delta)
  }
}
