package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.*
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.log.debug
import ktx.log.logger


class CollisionSystem(
  private val world: World
) : IteratingSystem(allOf(CollisionComponent::class).get()), ContactListener {

  override fun addedToEngine(engine: Engine?) {
    super.addedToEngine(engine)
    world.setContactListener(this)
  }

  override fun removedFromEngine(engine: Engine?) {
    super.removedFromEngine(engine)
    world.setContactListener(null)
  }

  private fun Any.isPlayerEntity() = this is Entity && this[PlayerComponent.MAPPER] != null && !this.isRemoving

  private fun Any.isActionableEntity() = this is Entity && this[ActionableComponent.MAPPER] != null && !this.isRemoving

  private fun forEachPlayerSensorCollision(contact: Contact, lambda: (Entity, Entity) -> Unit) {
    val userDataA = contact.fixtureA.body.userData
    val userDataB = contact.fixtureB.body.userData

    if (contact.fixtureA.isSensor && userDataA.isPlayerEntity() && userDataB.isActionableEntity()) {
      lambda(userDataA as Entity, userDataB as Entity)
    } else if (contact.fixtureB.isSensor && userDataB.isPlayerEntity() && userDataA.isActionableEntity()) {
      lambda(userDataB as Entity, userDataA as Entity)
    }
  }

  override fun beginContact(contact: Contact) {
    forEachPlayerSensorCollision(contact) { player, otherEntity ->
      val collisionCmp = player[CollisionComponent.MAPPER] ?: player.addAndReturn(
        engine.createComponent(
          CollisionComponent::class.java
        )
      )
      collisionCmp.beginContactEntities.add(otherEntity)
    }
  }

  override fun endContact(contact: Contact) {
    forEachPlayerSensorCollision(contact) { player, otherEntity ->
      val collisionCmp = player[CollisionComponent.MAPPER] ?: player.addAndReturn(
        engine.createComponent(
          CollisionComponent::class.java
        )
      )
      collisionCmp.endContactEntities.add(otherEntity)
    }
  }

  override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit

  override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val collisionCmp = entity.collisionCmp
    val interactCmp = entity[InteractComponent.MAPPER]

    LOG.debug { "beginContactEntities=${collisionCmp.beginContactEntities}" }
    LOG.debug { "endContactEntities=${collisionCmp.endContactEntities}" }
    if (interactCmp != null) {
      // entity can interact with actionable entities
      collisionCmp.beginContactEntities.forEach { collEntity ->
        interactCmp.entitiesInRange.add(collEntity)
      }
      collisionCmp.endContactEntities.forEach { collEntity ->
        interactCmp.entitiesInRange.remove(collEntity)
      }
    }

    entity.remove(CollisionComponent::class.java)
  }

  companion object {
    private val LOG = logger<CollisionSystem>()
  }
}
