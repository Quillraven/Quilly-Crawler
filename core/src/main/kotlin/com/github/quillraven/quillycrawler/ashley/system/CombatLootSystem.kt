package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.quillycrawler.ashley.component.CombatLootComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerComponent
import com.github.quillraven.quillycrawler.ashley.component.bagCmp
import com.github.quillraven.quillycrawler.ashley.component.combatLootCmp
import com.github.quillraven.quillycrawler.event.GameCombatLoot
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.allOf

class CombatLootSystem(
  private val gameEventDispatcher: GameEventDispatcher
) : IteratingSystem(allOf(PlayerComponent::class, CombatLootComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    with(entity.combatLootCmp) {
      entity.bagCmp.gold += this.gold
      gameEventDispatcher.dispatchEvent<GameCombatLoot> {
        this.gold = this@with.gold
        this.victory = this@with.victory
      }
    }
    entity.remove(CombatLootComponent::class.java)
  }
}
