package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.quillycrawler.ashley.component.CombatComponent
import ktx.ashley.allOf

class CombatSystem : IteratingSystem(allOf(CombatComponent::class).get()) {
  override fun processEntity(entity: Entity?, deltaTime: Float) {
    TODO("Not yet implemented")
  }
}
