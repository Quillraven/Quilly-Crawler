package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.*
import com.github.quillraven.commons.ashley.component.RenderComponent
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

  private fun forEachPlayerSensorCollision(contact: Contact, lambda: (Entity, Entity) -> Unit) {
    val entityA = contact.fixtureA.body.userData
    val entityB = contact.fixtureB.body.userData

    if (entityA is Entity && entityA[PlayerComponent.MAPPER] != null && contact.fixtureA.isSensor && entityB is Entity) {
      lambda(entityA, entityB)
    } else if (entityB is Entity && entityB[PlayerComponent.MAPPER] != null && contact.fixtureB.isSensor && entityA is Entity) {
      lambda(entityB, entityA)
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
      // entity can interact with other entities -> check if there is contact with an actionable entity
      collisionCmp.beginContactEntities.forEach { collEntity ->
        if (collEntity[ActionableComponent.MAPPER] != null) {
          interactCmp.entitiesInRange.add(collEntity)
          collEntity[RenderComponent.MAPPER]?.sprite?.setColor(1f, 0f, 0f, 1f)
        }
      }
      collisionCmp.endContactEntities.forEach { collEntity ->
        if (collEntity[ActionableComponent.MAPPER] != null) {
          interactCmp.entitiesInRange.remove(collEntity)
          collEntity[RenderComponent.MAPPER]?.sprite?.setColor(1f, 1f, 1f, 1f)
        }
      }
    }

    entity.remove(CollisionComponent::class.java)
  }

  companion object {
    private val LOG = logger<CollisionSystem>()
  }
}
