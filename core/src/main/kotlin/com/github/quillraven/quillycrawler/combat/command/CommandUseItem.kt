package com.github.quillraven.quillycrawler.combat.command

import com.badlogic.ashley.core.Entity
import com.github.quillraven.quillycrawler.ashley.component.ConsumeComponent
import com.github.quillraven.quillycrawler.combat.CombatContext
import ktx.ashley.configureEntity
import ktx.ashley.with

class CommandUseItem(context: CombatContext) : Command(context) {
  override val aiType = CommandAiType.SUPPORTIVE
  override val manaCost = 0
  override val targetType = CommandTargetType.NO_TARGET
  lateinit var itemToConsume: Entity

  override fun onStart() {
    engine.configureEntity(entity) {
      with<ConsumeComponent> {
        itemsToConsume.add(itemToConsume)
      }
    }
  }
}
