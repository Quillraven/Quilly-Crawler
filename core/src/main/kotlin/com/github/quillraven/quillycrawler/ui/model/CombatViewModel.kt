package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.renderCmp
import com.github.quillraven.commons.ashley.component.transformCmp
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.combat.command.CommandAttack
import com.github.quillraven.quillycrawler.combat.command.CommandDeath
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
  fun onLifeChange(life: Float, maxLife: Float) = Unit
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
  private val enemyEntities =
    engine.getEntitiesFor(allOf(CombatComponent::class, StatsComponent::class).exclude(PlayerComponent::class).get())
  private lateinit var playerEntity: Entity
  private val playerCommands = ObjectMap<String, KClass<out Command>>()
  private var selectedCommand: KClass<out Command>? = null
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

  fun executeOrder() {
    // TODO consume item logic : selectedItem

    val cmd = selectedCommand
    if (cmd == null) {
      LOG.error { "Cannot execute null command" }
      return
    }

    playerEntity.combatCmp.availableCommands.let { cmds ->
      if (!cmds.containsKey(cmd)) {
        LOG.error { "Command ${cmd.simpleName} is not part of available commands" }
        return
      }

      playerEntity.combatCmp.newCommand(cmd, selectedTarget)
    }

    selectedCommand = null
    selectedTarget = null
    selectedItem = null
  }

  fun returnToGame() {
    game.setScreen<GameScreen>()
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
      is CombatNewTurnEvent -> {
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
        playerEntity.combatCmp.availableCommands.keys().forEach { commandClass ->
          if (commandClass == CommandAttack::class || commandClass == CommandDeath::class) {
            return@forEach
          }

          val abilityName = try {
            bundle["Ability.${commandClass.simpleName}.name"]
          } catch (e: MissingResourceException) {
            LOG.error { "Ability ${commandClass.simpleName} has no name i18n property" }
            "UNKNOWN"
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
            val uiPosition = positionPool.obtain()
            val gamePosition = transformCmp.position
            uiPosition.set(gamePosition.x + transformCmp.size.x * 0.5f, gamePosition.y)
            gameViewport.project(uiPosition)
            uiViewport.unproject(uiPosition)
            uiPosition.y = uiViewport.worldHeight - uiPosition.y
            targets[uiPosition] = entity
            TMP_ARRAY_3.add(uiPosition)
          }
        }

        // notify view
        listeners.forEach { it.onNextTurn(event.turn, turnEntityImgs, TMP_ARRAY_1, TMP_ARRAY_2, TMP_ARRAY_3) }
      }
      is CombatVictoryEvent -> {
        combatState = CombatState.VICTORY
        audioService.play(MusicAssets.VICTORY, loop = false)
      }
      is CombatDefeatEvent -> {
        combatState = CombatState.DEFEAT
        audioService.play(MusicAssets.DEFEAT, loop = false)
      }
      is CombatPostDamageEvent -> {
        if (event.damageEmitterComponent.target == playerEntity) {
          event.damageEmitterComponent.target.statsCmp.let { statsCmp ->
            val maxLife = statsCmp.totalStatValue(playerEntity, StatsType.MAX_LIFE)
            val life = statsCmp.totalStatValue(playerEntity, StatsType.LIFE)
            listeners.forEach { it.onLifeChange(life, maxLife) }
          }
        }
      }
      else -> Unit
    }
  }

  companion object {
    private val LOG = logger<CombatViewModel>()
    private val TMP_ARRAY_1 = GdxArray<String>()
    private val TMP_ARRAY_2 = GdxArray<String>()
    private val TMP_ARRAY_3 = GdxArray<Vector2>()
  }
}
