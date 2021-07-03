package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.screen.GameScreen
import ktx.ashley.configureEntity
import ktx.ashley.with
import ktx.collections.GdxArray
import ktx.collections.GdxSet
import java.util.*

interface ShopListener {
  fun onItemsUpdated(items: GdxArray<String>, selectionIndex: Int) = Unit
}

enum class ShopMode {
  SELL, BUY
}

data class ShopItemViewModel(
  val amount: Int,
  val name: String,
  val description: String,
  val regionKey: String,
  val cost: Int
) {
  override fun toString(): String {
    return if (amount > 0) {
      "${cost}G - ${amount}x $name".take(23)
    } else {
      "${cost}G - $name".take(23)
    }
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
  //  item sell price is 75% of its original cost
  //  attribute tomes get more expensive every time you buy them (maybe separate component that stores the amount data? or compare current stats to base stats?)
  //  maybe we need to keep the stats table in the UI to show if an item increases/decreases the stats when the player wants to buy it
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

    entity.bagCmp.items.values().forEach { item ->
      itemEntities.add(item)
      item.itemCmp.also { itemCmp ->
        uiItems.add(
          ShopItemViewModel(
            itemCmp.amount,
            itemCmp.name(bundle),
            itemCmp.description(bundle),
            itemCmp.regionKey(bundle),
            if (mode == ShopMode.BUY) itemCmp.cost else (itemCmp.cost * SELL_RATIO).toInt()
          )
        )
      }
    }
  }

  fun setSellMode(): GdxArray<ShopItemViewModel> {
    mode = ShopMode.SELL
    updateItems()
    return uiItems
  }

  fun setBuyMode(): GdxArray<ShopItemViewModel> {
    mode = ShopMode.BUY
    updateItems()
    return uiItems
  }

  fun playerStats(): EnumMap<StatsType, StringBuilder> {
    val statsCmp = playerEntity.statsCmp
    StatsType.VALUES.forEach { type ->
      if (type == StatsType.MAX_LIFE || type == StatsType.MAX_MANA) {
        return@forEach
      }

      val strBuilder = statsInfo.getOrPut(type) { StringBuilder(20) }
      strBuilder.clear()
      when (type) {
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
          strBuilder.append(bundle[type.name]).append(": ")
            .append(statsCmp[type].toInt())
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
