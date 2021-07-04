package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.ashley.component.removeFromEngine
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.createItemEntity
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.screen.GameScreen
import ktx.ashley.configureEntity
import ktx.ashley.get
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import ktx.collections.contains
import ktx.collections.set
import java.util.*

interface ShopListener {
  fun onItemsUpdated(items: GdxArray<ShopItemViewModel>) = Unit
  fun onGoldUpdated(gold: Int) = Unit
}

enum class ShopMode {
  SELL, BUY
}

data class ShopItemViewModel(
  var amount: Int,
  val name: String,
  val description: String,
  val regionKey: String,
  val cost: Int,
  val equipped: Boolean,
  var canBuy: Boolean,
) {
  override fun toString(): String {
    var result = if (amount > 0) {
      // string in SELL mode
      "${cost}G - ${amount}x $name"
    } else {
      // string in BUY mode
      "${cost}G - $name"
    }

    var length = result.length
    if (equipped) {
      result += " [#5555ff]E[]"
      length++
    }

    result = if (length > 25) {
      if (equipped) {
        result.take(22) + ". [#5555ff]E[]"
      } else {
        result.take(24) + "."
      }
    } else {
      result
    }

    if (amount <= 0 && !canBuy) {
      return "[#ff0000]$result[]"
    }
    return result
  }
}

data class ShopViewModel(
  val bundle: I18NBundle,
  val engine: Engine,
  var playerEntity: Entity,
  var shopEntity: Entity,
  val audioService: AudioService
) {
  // TODO
  //  attribute tomes get more expensive every time you buy them (maybe separate component that stores the amount data? or compare current stats to base stats?)
  private val listeners = GdxSet<ShopListener>()
  private val statsInfo = EnumMap<StatsType, StringBuilder>(StatsType::class.java)
  private var mode = ShopMode.SELL
  private val uiItems = GdxArray<ShopItemViewModel>()
  private val itemEntities = GdxArray<Entity>()

  fun addShopListener(listener: ShopListener) = listeners.add(listener)

  fun removeShopListener(listener: ShopListener) = listeners.remove(listener)

  private fun updateItems() {
    val entity = if (mode == ShopMode.BUY) {
      shopEntity
    } else {
      playerEntity
    }

    uiItems.clear()
    itemEntities.clear()
    val gold = playerEntity.bagCmp.gold

    entity.bagCmp.items.values().forEach { item ->
      itemEntities.add(item)
      item.itemCmp.also { itemCmp ->
        uiItems.add(
          ShopItemViewModel(
            itemCmp.amount,
            itemCmp.name(bundle),
            itemCmp.description(bundle),
            itemCmp.regionKey(bundle),
            if (mode == ShopMode.BUY) itemCmp.cost else (itemCmp.cost * SELL_RATIO).toInt(),
            entity[GearComponent.MAPPER]?.gear?.containsKey(itemCmp.gearType) ?: false,
            if (mode == ShopMode.BUY) itemCmp.cost <= gold else true,
          )
        )
      }
    }
  }

  fun setSellMode(playSnd: Boolean = true): GdxArray<ShopItemViewModel> {
    if (playSnd && mode != ShopMode.SELL) {
      audioService.play(SoundAssets.MENU_BACK)
    }
    mode = ShopMode.SELL
    updateItems()
    return uiItems
  }

  fun setBuyMode(): GdxArray<ShopItemViewModel> {
    if (mode != ShopMode.BUY) {
      audioService.play(SoundAssets.MENU_BACK)
    }
    mode = ShopMode.BUY
    updateItems()
    return uiItems
  }

  fun updateSelection() {
    audioService.play(SoundAssets.MENU_SELECT)
  }

  fun selectItem(idx: Int) {
    if (idx == -1) {
      // nothing selected
      return
    }

    if (mode == ShopMode.SELL) {
      sellItem(idx)
    } else {
      buyItem(idx)
    }
  }

  private fun buyItem(idx: Int) {
    val uiItem = uiItems[idx]
    if (!uiItem.canBuy) {
      // not enough gold -> do nothing
      return
    }

    audioService.play(SoundAssets.MENU_SELECT_2)

    // enough gold -> buy item
    val item = itemEntities[idx]
    val selItemCmp = item.itemCmp
    val selItemType = selItemCmp.itemType
    val playerBagCmp = playerEntity.bagCmp
    if (selItemType in playerBagCmp.items) {
      playerBagCmp.items[selItemType].itemCmp.amount++
    } else {
      playerBagCmp.items[selItemType] = engine.createItemEntity(selItemType)
    }
    playerBagCmp.gold -= item.itemCmp.cost
    engine.update(0f)

    // update 'purchasable' info for UI items
    uiItems.forEach { it.canBuy = playerBagCmp.gold >= it.cost }
    listeners.forEach {
      it.onItemsUpdated(uiItems)
      it.onGoldUpdated(playerBagCmp.gold)
    }
  }

  private fun sellItem(idx: Int) {
    audioService.play(SoundAssets.MENU_SELECT_2)

    val item = itemEntities[idx]
    val itemCmp = item.itemCmp
    val playerBag = playerEntity.bagCmp
    playerBag.gold += (itemCmp.cost * SELL_RATIO).toInt()
    listeners.forEach { it.onGoldUpdated(playerBag.gold) }

    if (itemCmp.amount > 1) {
      itemCmp.amount--
      uiItems[idx].amount--
    } else {
      // no more items left -> remove it from bag
      playerBag.items.remove(itemCmp.itemType)
      item.removeFromEngine(engine)
      // this also triggers entityRemoved of the GearSystem
      engine.update(0f)
      uiItems.removeIndex(idx)
      itemEntities.removeIndex(idx)
    }

    listeners.forEach { it.onItemsUpdated(uiItems) }
  }

  fun playerStats(selectedItemIdx: Int): EnumMap<StatsType, StringBuilder> {
    val statsCmp = playerEntity.statsCmp
    val gearCmp = playerEntity.gearCmp
    StatsType.VALUES.forEach { statType ->
      if (statType == StatsType.MAX_LIFE || statType == StatsType.MAX_MANA) {
        return@forEach
      }

      val strBuilder = statsInfo.getOrPut(statType) { StringBuilder(20) }
      strBuilder.clear()

      // basic player stats
      when (statType) {
        StatsType.LIFE -> {
          strBuilder.append(bundle["LIFE"]).append(": ")
            .append(statsCmp[StatsType.LIFE].toInt())
            .append("/")
            .append(statsCmp[StatsType.MAX_LIFE].toInt())
        }
        StatsType.MANA -> {
          strBuilder.append(bundle["MANA"]).append(": ")
            .append(statsCmp[StatsType.MANA].toInt())
            .append("/")
            .append(statsCmp[StatsType.MAX_MANA].toInt())
        }
        else -> {
          strBuilder.append(bundle[statType.name]).append(": ")
            .append(statsCmp[statType].toInt())
        }
      }

      // gear stats
      var gearBonus = 0f
      gearCmp.gear.values().forEach { gearItem ->
        val typeToCheck = when (statType) {
          StatsType.LIFE -> StatsType.MAX_LIFE
          StatsType.MANA -> StatsType.MAX_MANA
          else -> statType
        }
        gearBonus += gearItem[StatsComponent.MAPPER]?.stats?.get(typeToCheck, 0f) ?: 0f
      }

      if (gearBonus > 0f) {
        strBuilder.append("[#54CC43]+").append(gearBonus.toInt()).append("[]")
      } else if (gearBonus < 0f) {
        strBuilder.append("[#FF4542]").append(gearBonus.toInt()).append("[]")
      }

      // stats comparison with current selected item in shop
      if (selectedItemIdx >= 0) {
        val selStats = itemEntities[selectedItemIdx][StatsComponent.MAPPER]
        val selItemCmp = itemEntities[selectedItemIdx].itemCmp
        val selGearType = selItemCmp.gearType
        if (selStats != null && selGearType != GearType.UNDEFINED) {
          // if it is a gear item then show the stats differences
          val playerGear = playerEntity.gearCmp.gear
          val diffValue = if (selGearType in playerGear) {
            if (mode == ShopMode.SELL && selItemCmp.amount == 1) {
              // selected item is a gear item and if sold, will be removed from the player
              when (statType) {
                StatsType.LIFE -> -selStats[StatsType.MAX_LIFE]
                StatsType.MANA -> -selStats[StatsType.MAX_MANA]
                else -> -selStats[statType]
              }
            } else {
              when (statType) {
                StatsType.LIFE -> playerGear[selGearType].statsCmp[StatsType.MAX_LIFE] - selStats[StatsType.MAX_LIFE]
                StatsType.MANA -> playerGear[selGearType].statsCmp[StatsType.MAX_MANA] - selStats[StatsType.MAX_MANA]
                else -> playerGear[selGearType].statsCmp[statType] - selStats[statType]
              }
            }
          } else {
            when (statType) {
              StatsType.LIFE -> selStats[StatsType.MAX_LIFE]
              StatsType.MANA -> selStats[StatsType.MAX_MANA]
              else -> selStats[statType]
            }
          }

          if (diffValue > 0f) {
            strBuilder.append(" [#434cFF]=>[]").append("[#54CC43]+").append(diffValue.toInt()).append("[]")
          } else if (diffValue < 0f) {
            strBuilder.append(" [#434cFF]=>[]").append("[#FF4542]").append(diffValue.toInt()).append("[]")
          }
        }
      }
    }

    return statsInfo
  }

  fun returnToGame() {
    engine.run {
      configureEntity(playerEntity) {
        with<SetScreenComponent> { screenType = GameScreen::class }
      }
      update(0f)
    }
  }

  fun gold(): Int {
    return playerEntity.bagCmp.gold
  }

  companion object {
    const val SELL_RATIO = 0.75f
  }
}
