package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.ashley.system.AnimationSystem
import com.github.quillraven.commons.ashley.system.RemoveSystem
import com.github.quillraven.commons.ashley.system.RenderSystem
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.system.CombatSystem
import com.github.quillraven.quillycrawler.ashley.system.ConsumeSystem
import com.github.quillraven.quillycrawler.ashley.system.DamageEmitterSystem
import com.github.quillraven.quillycrawler.ashley.system.SetScreenSystem
import com.github.quillraven.quillycrawler.ashley.withAnimationComponents
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import com.github.quillraven.quillycrawler.combat.CombatOrderEffectAttack
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.*
import ktx.collections.set

class CombatScreen(
  private val game: QuillyCrawler,
  var playerEntity: Entity,
  var enemyEntity: Entity,
  private val gameEventDispatcher: GameEventDispatcher = game.gameEventDispatcher
) : AbstractScreen(game), GameEventListener {
  private val gameViewport = game.gameViewport
  private val engine = PooledEngine().apply {
    addSystem(CombatSystem(audioService, gameEventDispatcher))
    addSystem(ConsumeSystem())
    addSystem(DamageEmitterSystem(gameEventDispatcher))
    addSystem(AnimationSystem(game.assetStorage, QuillyCrawler.UNIT_SCALE))
    addSystem(RenderSystem(game.batch, gameViewport))
    addSystem(SetScreenSystem(game))
    addSystem(RemoveSystem())
  }

  override fun show() {
    super.show()
    gameEventDispatcher.addListener(GameEventType.COMBAT_VICTORY, this)
    audioService.playMusic(MusicAssets.QUANTUM_LOOP.descriptor.fileName)
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
    engine.entity {
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
      with<BagComponent> {
        //TODO think about how to update the bag of the original player when an item is used in combat
        //e.g. iterate over playerEntity bag when screen gets hidden?
        playerEntity.bagCmp.items.forEach { entry -> items[entry.key] = entry.value }
      }
      with<GearComponent> { playerEntity.gearCmp.gear.forEach { entry -> gear[entry.key] = entry.value } }
      with<StatsComponent> { playerEntity.statsCmp.stats.forEach { entry -> stats[entry.key] = entry.value } }
      with<CombatComponent>()
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
        stats[StatsType.AGILITY] = 30f
        stats[StatsType.LIFE] = 15f
        stats[StatsType.PHYSICAL_DAMAGE] = 5f
      }
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with<CombatComponent>()
    }
  }

  override fun onEvent(event: GameEvent) {
    if (event is CombatVictoryEvent) {
      enemyEntity.removeFromEngine(engine)
      game.setScreen<GameScreen>()
    }
  }

  override fun render(delta: Float) {
    //TODO remove debug stuff
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach { entity ->
        engine.configureEntity(entity) {
          with<SetScreenComponent> { screenType = GameScreen::class }
        }
      }
    } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach {
        it.combatCmp.effect = CombatOrderEffectAttack
        it.combatCmp.orderTargets.add(
          engine.getEntitiesFor(
            allOf(CombatComponent::class).exclude(PlayerComponent::class).get()
          ).random()
        )
      }
    }

    engine.update(delta)
  }
}
