package com.github.quillraven.quillycrawler.preferences

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.utils.Json
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.quillycrawler.QuillyCrawler.Companion.PREF_KEY_SAVE_STATE
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.createItemEntity
import com.github.quillraven.quillycrawler.combat.command.Command
import ktx.ashley.configureEntity
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.set
import ktx.preferences.flush
import ktx.preferences.get
import ktx.preferences.set
import kotlin.reflect.KClass

private data class Item(
  val type: String = "",
  val amount: Int = 0,
  val equipped: Boolean = false,
)

private data class Stat(
  val type: String = "",
  val amount: Float = 0f,
)

private data class GameState(
  var dungeonLevel: Int = -1,
  var gold: Int = 0,
  val items: GdxArray<Item> = GdxArray(),
  val commands: GdxArray<String> = GdxArray(),
  val stats: GdxArray<Stat> = GdxArray(),
)

fun Preferences.saveGameState(engine: Engine) {
  val gameState = GameState()

  engine.entities.forEach { entity ->
    if (entity.isPlayer) {
      gameState.dungeonLevel = entity.playerCmp.dungeonLevel
      val gearCmp = entity.gearCmp
      with(entity.bagCmp) {
        gameState.gold = this.gold
        this.items.forEach { entry ->
          gameState.items.add(
            Item(
              entry.key.toString(),
              entry.value.itemCmp.amount,
              gearCmp.gear.containsKey(entry.key.gearType)
            )
          )
        }
      }
      with(entity.combatCmp) {
        this.commandsToLearn.forEach { cmdType ->
          gameState.commands.add(cmdType.qualifiedName)
        }
      }

      // stats
      with(entity.statsCmp) {
        stats.forEach { entry ->
          gameState.stats.add(Stat(entry.key.toString(), entry.value))
        }
      }

      // store game state in preferences
      this.flush {
        set(PREF_KEY_SAVE_STATE, Json().toJson(gameState))
      }

      return
    }
  }
}

fun Preferences.loadGameState(engine: Engine) {
  val gameState = Json().fromJson(GameState::class.java, this[PREF_KEY_SAVE_STATE, "{}"])

  engine.entities.forEach { entity ->
    if (entity.isPlayer) {
      val bagCmp = entity.bagCmp
      bagCmp.items.values().forEach { itemEntity ->
        itemEntity.removeFromEngine(engine)
      }
      entity.remove(BagComponent::class.java)
      entity.remove(GearComponent::class.java)
      entity.remove(CombatComponent::class.java)

      engine.configureEntity(entity) {
        // items
        with<GearComponent>()
        val equipCmp = with<EquipComponent>()
        with<BagComponent> {
          gold = gameState.gold
          gameState.items.forEach { item ->
            val itemType = ItemType.valueOf(item.type)
            val createdItem = engine.createItemEntity(itemType, item.amount)
            items[itemType] = createdItem
            if (item.equipped) {
              equipCmp.addToGear.add(createdItem)
            }
          }
        }

        // combat commands
        with<CombatComponent> {
          gameState.commands.forEach { cmd ->
            @Suppress("UNCHECKED_CAST")
            learn(Class.forName(cmd).kotlin as KClass<out Command>)
          }
        }

        // stats
        gameState.stats.forEach { stat ->
          entity.statsCmp[StatsType.valueOf(stat.type)] = stat.amount
        }

        // dungeon level
        // increase level by one for the MapSystem to correctly find the map folder
        entity.playerCmp.dungeonLevel = gameState.dungeonLevel + 1
        with<GoToLevel> {
          targetLevel = gameState.dungeonLevel
        }
      }

      return
    }
  }
}
