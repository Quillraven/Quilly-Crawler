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
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.command.*
import com.github.quillraven.quillycrawler.event.*
import com.github.quillraven.quillycrawler.screen.GameScreen
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
  fun onNextTurn(
    turn: Int,
    entityImages: GdxArray<Image>,
    abilities: GdxArray<String>,
    items: GdxArray<String>,
    targets: GdxArray<Vector2>
  )

  fun onVictory() = Unit
  fun onDefeat() = Unit
  fun onCombatStart(life: Float, maxLife: Float, mana: Float, maxMana: Float) = Unit
  fun onDamage(entityPos: Vector2, damage: Float) = Unit
  fun onLifeChange(life: Float, maxLife: Float) = Unit
  fun onManaChange(mana: Float, maxMana: Float) = Unit
}

enum class CombatState {
  RUNNING, DEFEAT, VICTORY
}

private class ImagePool : Pool<Image>() {
  override fun newObject(): Image = Image(TextureRegionDrawable(), Scaling.fit)
}

data class CombatViewModel(
  val bundle: I18NBundle,
  val engine: Engine,
  val game: QuillyCrawler,
  val gameViewport: Viewport = game.gameViewport,
  val uiViewport: Viewport = game.uiViewport,
  val audioService: AudioService = game.audioService
) : GameEventListener {
  private val listeners = GdxSet<CombatUiListener>()
  var combatState: CombatState = CombatState.RUNNING
  private lateinit var playerEntity: Entity
  private val enemyEntities =
    engine.getEntitiesFor(allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get())
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

  fun selectTarget(targetKey: Vector2) {
    selectedTarget = targets[targetKey]
  }

  fun selectAttackCommand() {
    selectedCommand = CommandAttack::class
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
    // TODO consume item logic : selectedItem
    val cmd = currentPlayerCommand()
    if (cmd.targetType == CommandTargetType.ALL_TARGETS) {
      playerEntity.combatCmp.newCommand(cmd::class, allEnemies())
    } else {
      playerEntity.combatCmp.newCommand(cmd::class, selectedTarget)
    }
    selectedCommand = CommandUnknown::class
    selectedTarget = null
    selectedItem = null
  }

  private fun allEnemies(): GdxArray<Entity> {
    TMP_ARRAY_4.clear()
    enemyEntities.forEach {
      if (it.isAlive) {
        TMP_ARRAY_4.add(it)
      }
    }
    return TMP_ARRAY_4
  }

  fun isSingleTargetCommand(): Boolean {
    return currentPlayerCommand().targetType == CommandTargetType.SINGLE_TARGET
  }

  fun returnToGame() {
    game.setScreen<GameScreen>()
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
        val uiPosition = entityUiPosition(transformCmp, transformCmp.size.x * 0.5f, transformCmp.size.y * 0.8f)
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
            listeners.forEach { it.onManaChange(mana, maxMana) }
          }
        }
      }
      else -> Unit
    }
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
    TMP_ARRAY_1.clear()
    val combatCmp = playerEntity.combatCmp
    combatCmp.availableCommands.keys().forEach { commandClass ->
      if (commandClass == CommandAttack::class || commandClass == CommandDeath::class) {
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
      TMP_ARRAY_1.add(abilityName)
    }

    // update player items for next turn
    playerItems.clear()
    TMP_ARRAY_2.clear()
    playerEntity.bagCmp.items.values().forEach { item ->
      if (item[ConsumableComponent.MAPPER] == null) {
        return@forEach
      }

      val itemCmp = item.itemCmp
      val itemName = bundle["Item.${itemCmp.itemType.name}.name"]
      playerItems[itemName] = item
      TMP_ARRAY_2.add(itemName)
    }

    // update possible targets for next turn
    targets.clear()
    TMP_ARRAY_3.iterate { vec2, iterator ->
      positionPool.free(vec2)
      iterator.remove()
    }
    event.turnEntities.forEach { entity ->
      if (!entity.isPlayer) {
        val transformCmp = entity.transformCmp
        val uiPosition = entityUiPosition(transformCmp, transformCmp.size.x * 0.5f)
        targets[uiPosition] = entity
        TMP_ARRAY_3.add(uiPosition)
      }
    }

    // notify view
    listeners.forEach { it.onNextTurn(event.turn, turnEntityImgs, TMP_ARRAY_1, TMP_ARRAY_2, TMP_ARRAY_3) }
  }

  companion object {
    private val LOG = logger<CombatViewModel>()
    private val TMP_ARRAY_1 = GdxArray<String>()
    private val TMP_ARRAY_2 = GdxArray<String>()
    private val TMP_ARRAY_3 = GdxArray<Vector2>()
    private val TMP_ARRAY_4 = GdxArray<Entity>()
    const val DISABLED_COMMAND = "[#cc2222]"
  }
}
