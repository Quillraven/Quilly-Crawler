package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.math.Vector2
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.StateComponent
import com.github.quillraven.commons.ashley.component.transformCmp
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.*
import ktx.collections.GdxSet
import ktx.collections.isNotEmpty
import ktx.log.error
import ktx.log.logger

class InteractSystem(
  private val messageManager: MessageManager
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
      interactCmp.interact = false

      // interact with closest entity
      val entityTransformCmp = entity.transformCmp
      TMP_VECTOR_1.set(entityTransformCmp.position.x, entityTransformCmp.position.y)
      val closestEntity = closestEntity(interactCmp.entitiesInRange)

      closestEntity[StateComponent.MAPPER]?.dispatchMessage(messageManager, MessageType.PLAYER_INTERACT.ordinal)
      doEntityAction(entity, closestEntity)
    }
  }

  private fun closestEntity(entitiesInRange: GdxSet<Entity>): Entity {
    var closestEntity = entitiesInRange.first()
    var lastDistance = -1f

    entitiesInRange.forEach { actionableEntity ->
      val actionableTransformCmp = actionableEntity.transformCmp
      TMP_VECTOR_2.set(actionableTransformCmp.position.x, actionableTransformCmp.position.y)
      val distance = TMP_VECTOR_1.dst2(TMP_VECTOR_2)

      if (lastDistance == -1f || lastDistance > distance) {
        lastDistance = distance
        closestEntity = actionableEntity
      }
    }

    return closestEntity
  }

  private fun doEntityAction(player: Entity, entity: Entity) {
    when (entity.actionableCmp.type) {
      ActionType.EXIT -> {
        player.add(engine.createComponent(GoToNextLevelComponent::class.java))
      }
      ActionType.CHEST -> {
        engine.configureEntity(player) {
          with<LootComponent> { lootType = LootType.COMMON }
        }
      }
      else -> {
        LOG.error { "Undefined ActionType '${entity.actionableCmp.type}'" }
      }
    }
  }

  companion object {
    private val LOG = logger<InteractSystem>()
    private val TMP_VECTOR_1 = Vector2()
    private val TMP_VECTOR_2 = Vector2()
  }
}
