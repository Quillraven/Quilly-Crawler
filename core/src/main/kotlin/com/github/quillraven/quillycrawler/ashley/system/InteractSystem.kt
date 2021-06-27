package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.StateComponent
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.GameInteractReaperEvent
import com.github.quillraven.quillycrawler.screen.CombatScreen
import ktx.ashley.*
import ktx.collections.isNotEmpty
import ktx.log.error
import ktx.log.logger

class InteractSystem(
  private val messageManager: MessageManager,
  private val audioService: AudioService,
  private val gameEventDispatcher: GameEventDispatcher,
) : EntityListener,
  IteratingSystem(allOf(PlayerComponent::class, InteractComponent::class).exclude(RemoveComponent::class).get()) {
  private val actionableFamily = allOf(ActionableComponent::class).get()

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    engine.addEntityListener(actionableFamily, this)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
  }

  override fun entityAdded(entity: Entity) = Unit

  override fun entityRemoved(entity: Entity) {
    // when an actionable entity gets removed from the game then it
    // also needs to be removed from any 'entitiesInRange' collection
    entities.forEach { it.interactCmp.entitiesInRange.remove(entity) }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val interactCmp = entity.interactCmp

    if (interactCmp.interact && interactCmp.entitiesInRange.isNotEmpty()) {
      // interact with closest entity
      interactCmp.closestEntityOrNull(entity)?.let { closestEntity ->
        closestEntity[StateComponent.MAPPER]?.dispatchMessage(messageManager, MessageType.PLAYER_INTERACT.ordinal)
        doEntityAction(entity, closestEntity)
        interactCmp.lastInteractEntity = closestEntity
      }
    }

    interactCmp.interact = false
  }

  private fun doEntityAction(player: Entity, entity: Entity) {
    when (entity.actionableCmp.type) {
      ActionType.EXIT -> {
        engine.configureEntity(player) {
          with<GoToLevel> {
            targetLevel = player.playerCmp.dungeonLevel + 1
          }
        }
        audioService.play(SoundAssets.POWER_UP_12)
      }
      ActionType.CHEST -> {
        engine.configureEntity(player) {
          with<LootComponent> { lootType = entity.lootCmp.lootType }
        }
        audioService.play(SoundAssets.CHEST_OPEN)
      }
      ActionType.ENEMY -> {
        engine.configureEntity(player) {
          with<SetScreenComponent> { screenType = CombatScreen::class }
        }
      }
      ActionType.REAPER -> {
        gameEventDispatcher.dispatchEvent<GameInteractReaperEvent> {
          this.entity = player
        }
      }
      ActionType.SHOP -> {
        // TODO change screen to shop screen
      }
      else -> {
        LOG.error { "Undefined ActionType '${entity.actionableCmp.type}'" }
      }
    }
  }

  companion object {
    private val LOG = logger<InteractSystem>()
  }
}
