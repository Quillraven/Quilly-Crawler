package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.system.AnimationSystem
import com.github.quillraven.commons.ashley.system.RemoveSystem
import com.github.quillraven.commons.ashley.system.RenderSystem
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.system.CombatSystem
import com.github.quillraven.quillycrawler.ashley.system.ConsumeSystem
import com.github.quillraven.quillycrawler.ashley.system.SetScreenSystem
import com.github.quillraven.quillycrawler.ashley.withAnimationComponents
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.ashley.allOf
import ktx.ashley.configureEntity
import ktx.ashley.entity
import ktx.ashley.with
import ktx.collections.set

class CombatScreen(
  game: QuillyCrawler,
  var playerEntity: Entity,
  var enemyEntity: Entity
) : AbstractScreen(game) {
  private val gameViewport = game.gameViewport
  private val engine = PooledEngine().apply {
    addSystem(CombatSystem())
    addSystem(ConsumeSystem())
    addSystem(AnimationSystem(game.assetStorage, QuillyCrawler.UNIT_SCALE))
    addSystem(RenderSystem(game.batch, gameViewport))
    addSystem(SetScreenSystem(game))
    addSystem(RemoveSystem())
  }

  override fun show() {
    // TODO change music
    super.show()
    createPlayerCombatEntity(playerEntity)
    createEnemyCombatEntities(enemyEntity, playerEntity.playerCmp.dungeonLevel)
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
    }
  }

  override fun hide() {
    super.hide()
    engine.removeAllEntities()
  }

  override fun render(delta: Float) {
    //TODO remove debug stuff
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      engine.getEntitiesFor(allOf(PlayerComponent::class).get()).forEach { entity ->
        engine.configureEntity(entity) {
          with<SetScreenComponent> { screenType = GameScreen::class }
        }
      }
    }

    engine.update(delta)
  }
}
