package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Scaling
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ui.*
import com.github.quillraven.quillycrawler.ui.model.ShopItemViewModel
import com.github.quillraven.quillycrawler.ui.model.ShopListener
import com.github.quillraven.quillycrawler.ui.model.ShopViewModel
import ktx.collections.GdxArray
import ktx.scene2d.*
import java.util.*
import com.badlogic.gdx.scenes.scene2d.ui.List as GdxList

class ShopView(private val viewModel: ShopViewModel, private val bundle: I18NBundle = viewModel.bundle) : View(),
  ShopListener {
  // sell / buy mode buttons
  private val btnSell: TextButton
  private val btnBuy: TextButton

  // item details
  private val goldLabel: Label
  private val itemsScrollPane: ScrollPane
  private val itemsToSellBuy: GdxList<ShopItemViewModel>
  private val itemImage: Image
  private val itemDescriptionLabel: Label

  // stats labels
  private val statsLabels = EnumMap<StatsType, Label>(StatsType::class.java)

  init {
    setFillParent(true)
    background = skin.getDrawable(SkinImages.WINDOW.regionKey)

    // header
    table {
      this@ShopView.btnSell = textButton(this@ShopView.bundle["ShopView.sell"], SkinTextButtonStyle.BRIGHT.name) { c ->
        c.padRight(5f)
      }
      this@ShopView.btnBuy = textButton(this@ShopView.bundle["ShopView.buy"], SkinTextButtonStyle.BRIGHT.name)
    }

    textButton(bundle["ShopView.title"], SkinTextButtonStyle.TITLE.name) { cell ->
      cell.expand()
        .top().padTop(8f)
        .height(25f).width(95f)
        .row()
    }

    // item bag table
    table { cell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)
      defaults().expand().fill()

      // gold info
      table { tableCell ->
        background = skin.getDrawable(SkinImages.FRAME_2.regionKey)

        image(SkinImages.GOLD.regionKey)
        this@ShopView.goldLabel = label("", SkinLabelStyle.DEFAULT.name) { labelCell ->
          labelCell.padLeft(3f)
        }

        tableCell.expand(false, false)
          .padTop(3f)
          .height(15f).width(65f)
          .row()
      }

      // items
      val itemTable = Table(skin).apply {
        defaults().expand().fill().pad(3f, 2f, 0f, 2f)
        this@ShopView.itemsToSellBuy = GdxList<ShopItemViewModel>(skin, SkinListStyle.DEFAULT.name)
        this.add(this@ShopView.itemsToSellBuy)
      }
      this@ShopView.itemsScrollPane = scrollPane(SkinScrollPaneStyle.NO_BGD.name) { spCell ->
        setScrollingDisabled(true, false)
        setScrollbarsVisible(false)

        actor = itemTable

        spCell.expand()
          .padBottom(3f)
      }

      cell.expand()
        .padBottom(3f)
        .width(95f).height(120f)
    }

    // item details and stats table
    table { tableCell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

      // item details
      this@ShopView.itemImage = image(skin.getDrawable(SkinImages.UNDEFINED.regionKey)) { cell ->
        setScaling(Scaling.fit)
        cell.top().padLeft(4f).padTop(6f).minWidth(20f)
      }
      this@ShopView.itemDescriptionLabel = label("", SkinLabelStyle.DEFAULT.name) { cell ->
        setAlignment(Align.left)
        wrap = true
        cell.expandX().fill()
          .padLeft(4f).padRight(4f).padTop(2f)
          .height(23f)
          .row()
      }

      // filler cell to move item info to top and stats to bottom
      table { tCell -> tCell.fill().expand().colspan(2).row() }

      // stats
      table { statsTableCell ->
        background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
        defaults().width(86f).fill().left()

        this@ShopView.statsLabels[StatsType.LIFE] = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.statsLabels[StatsType.PHYSICAL_DAMAGE] = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.statsLabels[StatsType.MANA] = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.statsLabels[StatsType.MAGIC_DAMAGE] = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.statsLabels[StatsType.PHYSICAL_ARMOR] = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.statsLabels[StatsType.MAGIC_ARMOR] = label("", SkinLabelStyle.DEFAULT.name) { it.row() }

        this@ShopView.statsLabels[StatsType.STRENGTH] =
          label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).row() }
        this@ShopView.statsLabels[StatsType.INTELLIGENCE] =
          label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).row() }
        this@ShopView.statsLabels[StatsType.AGILITY] =
          label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).padBottom(3f) }

        statsTableCell.expandX().fillX()
          .width(176f).height(51f)
          .pad(2f)
          .colspan(2)
      }

      tableCell.expand()
        .top()
        .padBottom(3f)
        .width(180f).height(120f)
        .row()
    }

    // controls table
    table { cell ->
      defaults().padRight(2f)

      table { tCell ->
        defaults().padRight(2f)

        image(skin.getDrawable(SkinImages.GAME_PAD_DOWN.regionKey))
        image(skin.getDrawable(SkinImages.GAME_PAD_UP.regionKey))
        image(skin.getDrawable(SkinImages.KEY_BOARD_DOWN.regionKey))
        image(skin.getDrawable(SkinImages.KEY_BOARD_UP.regionKey))
        label(this@ShopView.bundle["ShopView.navigateInfo1"], SkinLabelStyle.DEFAULT.name) { lblCell ->
          lblCell.row()
        }

        image(skin.getDrawable(SkinImages.GAME_PAD_LEFT.regionKey))
        image(skin.getDrawable(SkinImages.GAME_PAD_RIGHT.regionKey))
        image(skin.getDrawable(SkinImages.KEY_BOARD_LEFT.regionKey))
        image(skin.getDrawable(SkinImages.KEY_BOARD_RIGHT.regionKey))
        label(this@ShopView.bundle["ShopView.navigateInfo2"], SkinLabelStyle.DEFAULT.name)

        tCell.expand().left()
      }

      image(skin.getDrawable(SkinImages.GAME_PAD_A.regionKey)) { it.padLeft(10f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_SPACE.regionKey))
      label(this@ShopView.bundle["ShopView.navigateInfo3"], SkinLabelStyle.DEFAULT.name)

      image(skin.getDrawable(SkinImages.GAME_PAD_B.regionKey)) { it.padLeft(10f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_ESCAPE.regionKey))
      label(this@ShopView.bundle["ShopView.navigateInfo4"], SkinLabelStyle.DEFAULT.name)

      cell.expand().left()
        .colspan(2)
        .padLeft(12f).padBottom(6f)
    }

    // debugAll()
  }

  override fun onShow() {
    viewModel.addShopListener(this)

    // update gold info
    with(goldLabel) {
      text.setLength(0)
      text.append(viewModel.gold())
      invalidateHierarchy()
    }

    // set SELL mode as default
    setSellMode()
  }

  override fun onHide() {
    viewModel.removeShopListener(this)
  }

  private fun setSellMode() {
    itemsToSellBuy.setItems(viewModel.setSellMode())
    selectItem(0)
    btnBuy.label.removeSelectionEffect()
    btnSell.label.addSelectionEffect()
  }

  private fun setBuyMode() {
    itemsToSellBuy.setItems(viewModel.setBuyMode())
    selectItem(0)
    btnSell.label.removeSelectionEffect()
    btnBuy.label.addSelectionEffect()
  }

  private fun switchMode() {
    if (btnSell.label.actions.isEmpty) {
      setSellMode()
    } else {
      setBuyMode()
    }
  }

  private fun selectItem(idx: Int) {
    if (itemsToSellBuy.items.isEmpty) {
      itemImage.isVisible = false
      itemDescriptionLabel.setText("")
      itemsToSellBuy.selectedIndex = -1
      // update stats info
      val playerStats = viewModel.playerStats(-1)
      playerStats.forEach { entry ->
        statsLabels[entry.key]?.setText(entry.value)
      }
      return
    }

    itemsToSellBuy.selectedIndex = when {
      idx < 0 -> itemsToSellBuy.items.size - 1
      idx >= itemsToSellBuy.items.size -> 0
      else -> idx
    }

    // adjust scrolling
    itemsScrollPane.scrollPercentY = itemsToSellBuy.selectedIndex.toFloat() / itemsToSellBuy.items.size

    // update item info
    itemDescriptionLabel.setText(itemsToSellBuy.selected.description)
    itemImage.drawable = skin.getDrawable(itemsToSellBuy.selected.regionKey)
    itemImage.isVisible = itemsToSellBuy.selected.regionKey != SkinImages.UNDEFINED.regionKey

    // update stats info
    val playerStats = viewModel.playerStats(itemsToSellBuy.selectedIndex)
    playerStats.forEach { entry ->
      statsLabels[entry.key]?.setText(entry.value)
    }
  }

  override fun onGoldUpdated(gold: Int) {
    with(goldLabel) {
      text.setLength(0)
      text.append(gold)
      invalidateHierarchy()
    }
  }

  override fun onItemsUpdated(items: GdxArray<ShopItemViewModel>) {
    var newIdx = itemsToSellBuy.selectedIndex
    itemsToSellBuy.setItems(items)
    // need to set index to -1 because otherwise item is not correctly
    // highlighted if the index doesn't change
    itemsToSellBuy.selectedIndex = -1
    if (newIdx >= items.size) {
      newIdx = items.size - 1
    }
    selectItem(newIdx)
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.DOWN -> {
        selectItem(itemsToSellBuy.selectedIndex + 1)
        viewModel.updateSelection()
      }
      Input.Keys.UP -> {
        selectItem(itemsToSellBuy.selectedIndex - 1)
        viewModel.updateSelection()
      }
      Input.Keys.LEFT, Input.Keys.RIGHT -> switchMode()
      Input.Keys.SPACE -> viewModel.selectItem(itemsToSellBuy.selectedIndex)
      Input.Keys.ESCAPE -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_DOWN -> {
        selectItem(itemsToSellBuy.selectedIndex + 1)
        viewModel.updateSelection()
      }
      XboxInputProcessor.BUTTON_UP -> {
        selectItem(itemsToSellBuy.selectedIndex - 1)
        viewModel.updateSelection()
      }
      XboxInputProcessor.BUTTON_LEFT, XboxInputProcessor.BUTTON_RIGHT -> switchMode()
      XboxInputProcessor.BUTTON_A -> viewModel.selectItem(itemsToSellBuy.selectedIndex)
      XboxInputProcessor.BUTTON_B -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }
}
