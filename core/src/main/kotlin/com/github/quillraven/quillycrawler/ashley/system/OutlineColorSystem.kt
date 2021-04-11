package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.allOf

class OutlineColorSystem : IteratingSystem(allOf(InteractComponent::class, PlayerComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val interactCmp = entity.interactCmp
    val closestEntity = interactCmp.closestEntityOrNull(entity)

    interactCmp.entitiesInRange.forEach { entityInRange ->
      val actionableCmp = entityInRange.actionableCmp

      actionableCmp.outlineColor.set(
        when (actionableCmp.type) {
          ActionType.CHEST -> if (entityInRange == closestEntity) NEUTRAL_CLOSEST_OUTLINE_COLOR else NEUTRAL_OUTLINE_COLOR
          ActionType.ENEMY -> if (entityInRange == closestEntity) ENEMY_CLOSEST_OUTLINE_COLOR else ENEMY_OUTLINE_COLOR
          else -> actionableCmp.outlineColor
        }
      )
    }
  }

  companion object {
    private val NEUTRAL_OUTLINE_COLOR = Color(0.074f, 0.682f, 0f, 0.75f)
    private val NEUTRAL_CLOSEST_OUTLINE_COLOR = Color(0.109f, 1f, 0f, 1f)
    private val ENEMY_OUTLINE_COLOR = Color(0.776f, 0.117f, 0f, 0.75f)
    private val ENEMY_CLOSEST_OUTLINE_COLOR = Color(1f, 0.153f, 0f, 1f)
  }
}
