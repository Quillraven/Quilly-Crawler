package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.event.CombatClearCommandsEvent
import com.github.quillraven.quillycrawler.event.CombatCommandAddedEvent
import com.github.quillraven.quillycrawler.event.CombatCommandPlayerEvent
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.contains
import ktx.log.error
import ktx.log.logger
import kotlin.reflect.KClass

class CombatComponent : Component, Pool.Poolable {
  lateinit var eventDispatcher: GameEventDispatcher
  val availableCommands = ObjectMap<KClass<out Command>, Command>()
  val commandsToLearn = GdxSet<KClass<out Command>>()
  var defeated = false

  inline fun <reified T : Command> newCommand(
    target: Entity? = null,
    clearPreviousCommands: Boolean = false,
    noinline action: T.() -> Unit = {}
  ) {
    @Suppress("UNCHECKED_CAST")
    newCommand(T::class, target, clearPreviousCommands, action as Command.() -> Unit)
  }

  fun newCommand(
    type: KClass<out Command>,
    target: Entity? = null,
    clearPreviousCommands: Boolean = false,
    action: Command.() -> Unit = {}
  ) {
    if (type in availableCommands) {
      availableCommands[type].apply {
        targets.clear()
        if (target != null) {
          targets.add(target)
        }
        action(this)
        newCommand(this, clearPreviousCommands)
      }
    } else {
      LOG.error { "${type.simpleName} is not part of available commands" }
    }
  }

  fun newCommand(
    type: KClass<out Command>,
    targetEntities: GdxArray<Entity>,
    clearPreviousCommands: Boolean = false,
    action: Command.() -> Unit = {}
  ) {
    if (type in availableCommands) {
      availableCommands[type].apply {
        targets.clear()
        targets.addAll(targetEntities)
        action(this)
        newCommand(this, clearPreviousCommands)
      }
    } else {
      LOG.error { "${type.simpleName} is not part of available commands" }
    }
  }

  fun newCommand(command: Command, clearPreviousCommands: Boolean = false) {
    if (clearPreviousCommands) {
      eventDispatcher.dispatchEvent<CombatClearCommandsEvent> { this.entity = command.entity }
    }

    if (command.entity.isPlayer && command.entity[CombatAIComponent.MAPPER] == null) {
      // player controlled entity command -> send event to CombatSystem that decision was made
      eventDispatcher.dispatchEvent<CombatCommandPlayerEvent> { this.command = command }
    }

    eventDispatcher.dispatchEvent<CombatCommandAddedEvent> { this.command = command }
  }

  inline fun <reified T : Command> learn() {
    commandsToLearn.add(T::class)
  }

  fun learn(cmdType: KClass<out Command>) {
    commandsToLearn.add(cmdType)
  }

  override fun reset() {
    availableCommands.clear()
    commandsToLearn.clear()
    defeated = false
  }

  companion object {
    val MAPPER = mapperFor<CombatComponent>()

    @PublishedApi
    internal val LOG = logger<CombatComponent>()
  }
}

val Entity.combatCmp: CombatComponent
  get() = this[CombatComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatComponent for entity '$this' is null")

val Entity.isPlayerCombatEntity: Boolean
  get() = this.isPlayer && this[CombatComponent.MAPPER] != null
