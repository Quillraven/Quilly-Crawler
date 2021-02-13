package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.*
import com.github.quillraven.commons.ashley.component.PlayerComponent
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.quillycrawler.ashley.component.CollectableComponent
import com.github.quillraven.quillycrawler.ashley.component.CollectingComponent
import com.github.quillraven.quillycrawler.ashley.component.CollisionComponent
import com.github.quillraven.quillycrawler.ashley.component.collisionCmp
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.log.debug
import ktx.log.logger

private object PlayerEntityContact {
    lateinit var player: Entity
    lateinit var otherEntity: Entity
}

class CollisionSystem(
    private val world: World
) : IteratingSystem(allOf(CollisionComponent::class).get()), ContactListener {
    private val playerEntityContact = PlayerEntityContact

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
        world.setContactListener(this)
    }

    override fun removedFromEngine(engine: Engine?) {
        super.removedFromEngine(engine)
        world.setContactListener(null)
    }

    private fun updatePlayerEntityContact(contact: Contact): Boolean {
        val entityA = contact.fixtureA.body.userData
        val entityB = contact.fixtureB.body.userData

        if (entityA is Entity && entityA[PlayerComponent.MAPPER] != null && contact.fixtureA.isSensor && entityB is Entity) {
            playerEntityContact.player = entityA
            playerEntityContact.otherEntity = entityB
        } else if (entityB is Entity && entityB[PlayerComponent.MAPPER] != null && contact.fixtureB.isSensor && entityA is Entity) {
            playerEntityContact.player = entityB
            playerEntityContact.otherEntity = entityA
        } else {
            return false
        }
        return true
    }

    override fun beginContact(contact: Contact) {
        if (!updatePlayerEntityContact(contact)) {
            return
        }

        // from here on we know it is a collision with the player entity's sensor
        // we are only interested for now if it is the player's sensor that collides
        val collisionCmp = playerEntityContact.player[CollisionComponent.MAPPER]
        if (collisionCmp == null) {
            playerEntityContact.player.add(engine.createComponent(CollisionComponent::class.java).apply {
                beginContactEntities.add(playerEntityContact.otherEntity)
            })
        } else {
            collisionCmp.beginContactEntities.add(playerEntityContact.otherEntity)
        }
    }

    override fun endContact(contact: Contact) {
        if (!updatePlayerEntityContact(contact)) {
            return
        }

        // from here on we know it is a collision with the player entity's sensor
        // we are only interested for now if it is the player's sensor that collides
        val collisionCmp = playerEntityContact.player[CollisionComponent.MAPPER]
        if (collisionCmp == null) {
            playerEntityContact.player.add(engine.createComponent(CollisionComponent::class.java).apply {
                endContactEntities.add(playerEntityContact.otherEntity)
            })
        } else {
            collisionCmp.endContactEntities.add(playerEntityContact.otherEntity)
        }
    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit

    override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val collisionCmp = entity.collisionCmp
        val collectingCmp = entity[CollectingComponent.MAPPER]

        LOG.debug { "beginContactEntities=${collisionCmp.beginContactEntities}" }
        LOG.debug { "endContactEntities=${collisionCmp.endContactEntities}" }
        if (collectingCmp != null) {
            // entity can collect other entities -> check if there is an updated contact with a collectable entity
            collisionCmp.beginContactEntities.forEach { collEntity ->
                if (collEntity[CollectableComponent.MAPPER] != null) {
                    collectingCmp.entitiesInRange.add(collEntity)
                    collEntity[RenderComponent.MAPPER]?.sprite?.setColor(1f, 0f, 0f, 1f)
                }
            }
            collisionCmp.endContactEntities.forEach { collEntity ->
                if (collEntity[CollectableComponent.MAPPER] != null) {
                    collectingCmp.entitiesInRange.remove(collEntity)
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