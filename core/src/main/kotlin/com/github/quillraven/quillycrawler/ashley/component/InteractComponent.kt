package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.component.transformCmp
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxSet

class InteractComponent : Component, Pool.Poolable {
  val entitiesInRange = GdxSet<Entity>()
  var interact = false

  override fun reset() {
    entitiesInRange.clear()
    interact = false
  }

  fun closestEntityOrNull(entity: Entity): Entity? {
    if (entitiesInRange.isEmpty) {
      return null
    }

    var closestEntity: Entity = entitiesInRange.first()
    var lastDistance = -1f

    val entityTransformCmp = entity.transformCmp
    TMP_VECTOR_1.set(entityTransformCmp.position.x, entityTransformCmp.position.y)

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

  companion object {
    val MAPPER = mapperFor<InteractComponent>()
    private val TMP_VECTOR_1 = Vector2()
    private val TMP_VECTOR_2 = Vector2()
  }
}

val Entity.interactCmp: InteractComponent
  get() = this[InteractComponent.MAPPER]
    ?: throw GdxRuntimeException("InteractComponent for entity '$this' is null")
