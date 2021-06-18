package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.renderCmp
import com.github.quillraven.commons.ashley.component.transformCmp
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.command.*
import com.github.quillraven.quillycrawler.event.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.iterate
import ktx.collections.set
import ktx.log.error
import ktx.log.logger
import java.util.*
import kotlin.reflect.KClass

interface CombatUiListener {
  fun onNextTurn(turn: Int, entityImages: GdxArray<Image>, abilities: GdxArray<String>, items: GdxArray<ItemViewModel>)
  fun onVictory() = Unit
  fun onDefeat() = Unit
  fun onCombatStart(life: Float, maxLife: Float, mana: Float, maxMana: Float) = Unit
  fun onDamage(entityPos: Vector2, damage: Float) = Unit
  fun onLifeChange(life: Float, maxLife: Float) = Unit
  fun onManaChange(mana: Float, maxMana: Float) = Unit
  fun onHeal(entityPos: Vector2, life: Float, mana: Float) = Unit
  fun onBuffsUpdated(entityPos: Vector2, buffRegionKeys: GdxArray<String>)
}

enum class CombatState {
  RUNNING, DEFEAT, VICTORY
}

private class ImagePool : Pool<Image>() {
  override fun newObject(): Image = Image(TextureRegionDrawable(), Scaling.fit)
}

data class ItemViewModel(
  val itemName: String,
  val amount: Int
) {
  override fun toString(): String {
    return "${amount}x $itemName".take(11)
  }
}

data class CombatViewModel(
  val bundle: I18NBundle,
  val engine: Engine,
  val game: QuillyCrawler,
  val returnToGameScreen: () -> Unit,
  val gameViewport: Viewport = game.gameViewport,
  val uiViewport: Viewport = game.uiViewport,
  val audioService: AudioService = game.audioService
) : GameEventListener {
  private val listeners = GdxSet<CombatUiListener>()
  var combatState: CombatState = CombatState.RUNNING
  private lateinit var playerEntity: Entity
  private val enemyEntities =
    engine.getEntitiesFor(allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get())
  private val playerEntities =
    engine.getEntitiesFor(allOf(CombatComponent::class, StatsComponent::class, PlayerComponent::class).get())
  private val playerCommands = ObjectMap<String, KClass<out Command>>()
  private var selectedCommand: KClass<out Command> = CommandUnknown::class
  private val playerItems = ObjectMap<String, Entity>()
  private var selectedItem: Entity? = null
  private val targets = ObjectMap<Vector2, Entity>()
  private var selectedTarget: Entity? = null
  private val imgPool = ImagePool()
  private val turnEntityImgs = GdxArray<Image>()
  private val positionPool = ReflectionPool(Vector2::class.java)

  fun addCombatListener(listener: CombatUiListener) = listeners.add(listener)

  fun removeCombatListener(listener: CombatUiListener) = listeners.remove(listener)

  fun navigate() = audioService.play(SoundAssets.MENU_SELECT)

  fun select() = audioService.play(SoundAssets.MENU_SELECT_2)

  fun navigateBack() = audioService.play(SoundAssets.MENU_BACK)

  fun selectTarget(targetKey: Vector2) {
    selectedTarget = targets[targetKey]
  }

  fun selectAttackCommand() {
    selectedCommand = CommandAttack::class
  }

  fun selectItemCommand() {
    selectedCommand = CommandUseItem::class
  }

  fun selectCommand(cmdKey: String) {
    selectedCommand = playerCommands[cmdKey]
  }

  fun selectItem(itemKey: String) {
    selectedItem = playerItems[itemKey]
  }

  private fun currentPlayerCommand(): Command {
    val cmd = selectedCommand
    if (cmd == CommandUnknown::class) {
      throw GdxRuntimeException("Cannot execute unknown Command")
    }

    playerEntity.combatCmp.availableCommands.let { cmds ->
      if (!cmds.containsKey(cmd)) {
        throw GdxRuntimeException("Command ${cmd.simpleName} is not part of available commands")
      }

      return playerEntity.combatCmp.availableCommands[cmd]
    }
  }

  fun executeOrder() {
    val cmd = currentPlayerCommand()
    if (cmd is CommandUseItem) {
      val itemToUse = selectedItem
      if (itemToUse == null) {
        LOG.error { "Cannot use a null item" }
      } else {
        cmd.itemToConsume = itemToUse
      }
      playerEntity.combatCmp.newCommand(cmd)
    } else {
      if (cmd.targetType == CommandTargetType.ALL_TARGETS) {
        playerEntity.combatCmp.newCommand(cmd::class, allEnemies())
      } else {
        playerEntity.combatCmp.newCommand(cmd::class, selectedTarget)
      }
    }
    selectedCommand = CommandUnknown::class
    selectedTarget = null
    selectedItem = null
  }

  private fun allEnemies(): GdxArray<Entity> {
    TMP_ENTITY_ARRAY.clear()
    enemyEntities.forEach {
      if (it.isAlive) {
        TMP_ENTITY_ARRAY.add(it)
      }
    }
    return TMP_ENTITY_ARRAY
  }

  private fun allPlayers(): GdxArray<Entity> {
    TMP_ENTITY_ARRAY.clear()
    playerEntities.forEach {
      if (it.isAlive) {
        TMP_ENTITY_ARRAY.add(it)
      }
    }
    return TMP_ENTITY_ARRAY
  }

  fun commandTargets(): GdxArray<Vector2> {
    // clear previous targets
    targets.clear()
    TARGET_POSITION_ARRAY.iterate { vec2, iterator ->
      positionPool.free(vec2)
      iterator.remove()
    }

    // get new targets
    val targetEntities = when (currentPlayerCommand().aiType) {
      CommandAiType.SUPPORTIVE, CommandAiType.DEFENSIVE -> allPlayers()
      else -> allEnemies()
    }
    targetEntities.forEach { entity ->
      val transformCmp = entity.transformCmp
      val uiPosition = entityUiPosition(transformCmp, transformCmp.width * 0.5f)
      targets[uiPosition] = entity
      TARGET_POSITION_ARRAY.add(uiPosition)
    }

    return TARGET_POSITION_ARRAY
  }

  fun isSingleTargetCommand(): Boolean {
    return currentPlayerCommand().targetType == CommandTargetType.SINGLE_TARGET
  }

  fun returnToGame() {
    returnToGameScreen()
  }

  private fun entityUiPosition(transformCmp: TransformComponent, offsetX: Float = 0f, offsetY: Float = 0f): Vector2 {
    val uiPosition = positionPool.obtain()
    val gamePosition = transformCmp.position
    uiPosition.set(gamePosition.x + offsetX, gamePosition.y + offsetY)
    gameViewport.project(uiPosition)
    uiViewport.unproject(uiPosition)
    uiPosition.y = uiViewport.worldHeight - uiPosition.y
    return uiPosition
  }

  override fun onEvent(event: GameEvent) {
    when (event) {
      is CombatStartEvent -> {
        playerEntity = event.playerEntity
        val statsCmp = playerEntity.statsCmp
        val maxLife = statsCmp.totalStatValue(playerEntity, StatsType.MAX_LIFE)
        val life = statsCmp.totalStatValue(playerEntity, StatsType.LIFE)
        val maxMana = statsCmp.totalStatValue(playerEntity, StatsType.MAX_MANA)
        val mana = statsCmp.totalStatValue(playerEntity, StatsType.MANA)

        listeners.forEach { it.onCombatStart(life, maxLife, mana, maxMana) }
      }
      is CombatNewTurnEvent -> onNewTurnEvent(event)
      is CombatVictoryEvent -> {
        combatState = CombatState.VICTORY
        audioService.play(MusicAssets.VICTORY, loop = false)
        listeners.forEach { it.onVictory() }
      }
      is CombatDefeatEvent -> {
        combatState = CombatState.DEFEAT
        audioService.play(MusicAssets.DEFEAT, loop = false)
        listeners.forEach { it.onDefeat() }
      }
      is CombatPostDamageEvent -> {
        val damEmitCmp = event.damageEmitterComponent

        // show damage floating text
        val transformCmp = damEmitCmp.target.transformCmp
        val uiPosition = entityUiPosition(transformCmp, transformCmp.width * 0.5f, transformCmp.height * 0.8f)
        listeners.forEach { it.onDamage(uiPosition, damEmitCmp.physicalDamage + damEmitCmp.magicDamage) }
        positionPool.free(uiPosition)

        if (damEmitCmp.target == playerEntity) {
          playerEntity.statsCmp.let { statsCmp ->
            val maxLife = statsCmp.totalStatValue(playerEntity, StatsType.MAX_LIFE)
            val life = statsCmp.totalStatValue(playerEntity, StatsType.LIFE)
            listeners.forEach { it.onLifeChange(life, maxLife) }
          }
        }
      }
      is CombatCommandStarted -> {
        if (event.command.entity == playerEntity) {
          playerEntity.statsCmp.let { statsCmp ->
            val maxMana = statsCmp.totalStatValue(playerEntity, StatsType.MAX_MANA)
            val mana = statsCmp.totalStatValue(playerEntity, StatsType.MANA)
            listeners.forEach {
              // show floating text for lost mana
              val transformCmp = playerEntity.transformCmp
              val uiPosition = entityUiPosition(transformCmp, transformCmp.width * 0.5f, transformCmp.height * 0.8f)
              it.onHeal(uiPosition, 0f, -event.command.manaCost.toFloat())
              positionPool.free(uiPosition)

              it.onManaChange(mana, maxMana)
            }
          }
        }
      }
      is CombatConsumeItemEvent -> onHealEvent(
        event.statsCmp[StatsType.LIFE],
        event.statsCmp[StatsType.MANA],
        event.entity
      )
      is CombatPostHealEvent -> onHealEvent(
        event.healEmitterComponent.life,
        event.healEmitterComponent.mana,
        event.healEmitterComponent.target
      )
      is CombatBuffAdded -> {
        if (event.buff.entity == playerEntity) {
          onEntityBuffsUpdated(event.buff.entity)
        }
      }
      is CombatBuffRemoved -> {
        if (event.buff.entity == playerEntity) {
          onEntityBuffsUpdated(event.buff.entity)
        }
      }
      else -> Unit
    }
  }

  private fun onHealEvent(
    life: Float,
    mana: Float,
    entity: Entity
  ) {
    if (life <= 0f && mana <= 0f) {
      return
    }

    // show healing floating text
    val transformCmp = entity.transformCmp
    val uiPosition = entityUiPosition(transformCmp, transformCmp.width * 0.5f, transformCmp.height * 0.8f)
    listeners.forEach { it.onHeal(uiPosition, life, mana) }
    positionPool.free(uiPosition)

    // update player life/mana bar
    if (entity == playerEntity) {
      playerEntity.statsCmp.let { statsCmp ->
        val maxLife = statsCmp.totalStatValue(playerEntity, StatsType.MAX_LIFE)
        val playerLife = statsCmp.totalStatValue(playerEntity, StatsType.LIFE)
        val maxMana = statsCmp.totalStatValue(playerEntity, StatsType.MAX_MANA)
        val playerMana = statsCmp.totalStatValue(playerEntity, StatsType.MANA)
        listeners.forEach {
          it.onLifeChange(playerLife, maxLife)
          it.onManaChange(playerMana, maxMana)
        }
      }
    }
  }

  private fun onEntityBuffsUpdated(entity: Entity) {
    TMP_STRING_ARRAY.clear()
    playerEntity.buffCmp.buffs.values().forEach { buff ->
      val buffRegionKey = try {
        bundle["Buff.${buff::class.simpleName}.skinRegionKey"]
      } catch (e: MissingResourceException) {
        LOG.error { "Buff ${buff::class.simpleName} has no skin region key" }
        "undefined"
      }
      TMP_STRING_ARRAY.add(buffRegionKey)
    }

    val transformCmp = entity.transformCmp
    val uiPosition = entityUiPosition(transformCmp, 0f, -0.75f)
    listeners.forEach { it.onBuffsUpdated(uiPosition, TMP_STRING_ARRAY) }
    positionPool.free(uiPosition)
  }

  private fun onNewTurnEvent(event: CombatNewTurnEvent) {
    // get entity images to show in which order they execute their commands
    turnEntityImgs.iterate { image, iterator ->
      imgPool.free(image)
      iterator.remove()
    }
    event.turnEntities.forEach { entity ->
      val sprite = entity.renderCmp.sprite
      turnEntityImgs.add(imgPool.obtain().apply {
        (drawable as TextureRegionDrawable).region = sprite
        color.set(sprite.color)
      })
    }

    // update player abilities for next turn
    playerCommands.clear()
    TMP_STRING_ARRAY.clear()
    val combatCmp = playerEntity.combatCmp
    combatCmp.availableCommands.keys().forEach { commandClass ->
      if (commandClass == CommandAttack::class || commandClass == CommandDeath::class || commandClass == CommandUseItem::class) {
        return@forEach
      }

      var abilityName = try {
        bundle["Ability.${commandClass.simpleName}.name"]
      } catch (e: MissingResourceException) {
        LOG.error { "Ability ${commandClass.simpleName} has no name i18n property" }
        "UNKNOWN"
      }

      if (!combatCmp.availableCommands[commandClass].hasSufficientMana()) {
        abilityName = "$DISABLED_COMMAND$abilityName[]"
      }
      playerCommands[abilityName] = commandClass
      TMP_STRING_ARRAY.add(abilityName)
    }

    // update player items for next turn
    playerItems.clear()
    ITEM_ARRAY.clear()
    playerEntity.bagCmp.items.values().forEach { item ->
      if (item[ConsumableComponent.MAPPER] == null) {
        return@forEach
      }

      val itemCmp = item.itemCmp
      val itemName = bundle["Item.${itemCmp.itemType.name}.name"]
      playerItems[itemName] = item
      ITEM_ARRAY.add(ItemViewModel(itemName, itemCmp.amount))
    }

    // notify view
    listeners.forEach { it.onNextTurn(event.turn, turnEntityImgs, TMP_STRING_ARRAY, ITEM_ARRAY) }
  }

  companion object {
    private val LOG = logger<CombatViewModel>()
    private val TMP_STRING_ARRAY = GdxArray<String>()
    private val ITEM_ARRAY = GdxArray<ItemViewModel>()
    private val TARGET_POSITION_ARRAY = GdxArray<Vector2>()
    private val TMP_ENTITY_ARRAY = GdxArray<Entity>()
    const val DISABLED_COMMAND = "[#cc2222]"
  }
}
