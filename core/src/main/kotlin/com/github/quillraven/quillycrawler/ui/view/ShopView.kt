package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Scaling
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinListStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.ShopListener
import com.github.quillraven.quillycrawler.ui.model.ShopViewModel
import ktx.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.List as GdxList

class ShopView(private val viewModel: ShopViewModel, private val bundle: I18NBundle = viewModel.bundle) : View(),
  ShopListener {
  // item details
  private val goldLabel: Label
  private val bagItems: GdxList<String>
  private val itemImage: Image
  private val itemDescriptionLabel: Label

  // gear labels
  private val helmetLabel: Label
  private val amuletLabel: Label
  private val armorLabel: Label
  private val weaponLabel: Label
  private val shieldLabel: Label
  private val glovesLabel: Label
  private val bootsLabel: Label

  // stats labels
  private val lifeLabel: Label
  private val manaLabel: Label
  private val strengthLabel: Label
  private val agilityLabel: Label
  private val intelligenceLabel: Label
  private val physDamageLabel: Label
  private val physArmorLabel: Label
  private val magicDamageLabel: Label
  private val magicArmorLabel: Label

  init {
    setFillParent(true)
    background = skin.getDrawable(SkinImages.WINDOW.regionKey)

    // header
    textButton(bundle["InventoryView.title"], SkinTextButtonStyle.TITLE.name) { cell ->
      cell.expand()
        .top().padTop(8f)
        .height(25f).width(95f)
        .colspan(2)
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
      this@ShopView.bagItems =
        listWidget<String>(SkinListStyle.DEFAULT.name).cell(padTop = 3f, padLeft = 2f, padRight = 2f)

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

      // gear
      table { gearTableCell ->
        background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
        defaults().width(40f).expandX().fill().left()

        this@ShopView.helmetLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.amuletLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.armorLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.shieldLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.bootsLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.glovesLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.weaponLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }

        gearTableCell.expand().fill().colspan(2)
          .padTop(2f)
          .width(176f).height(34f)
          .row()
      }

      // stats
      table { statsTableCell ->
        background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
        defaults().width(86f).fill().left()

        this@ShopView.lifeLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.physDamageLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.manaLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.magicDamageLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@ShopView.physArmorLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@ShopView.magicArmorLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }

        this@ShopView.strengthLabel = label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).row() }
        this@ShopView.intelligenceLabel =
          label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).row() }
        this@ShopView.agilityLabel =
          label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).padBottom(3f) }

        statsTableCell.expandX().fillX()
          .width(176f).height(51f)
          .pad(2f)
          .colspan(2)
      }

      tableCell.expand()
        .padBottom(3f)
        .width(180f).height(120f)
        .row()
    }

    // controls table
    table { cell ->
      defaults().padRight(2f)

      image(skin.getDrawable(SkinImages.GAME_PAD_DOWN.regionKey))
      image(skin.getDrawable(SkinImages.GAME_PAD_UP.regionKey))
      image(skin.getDrawable(SkinImages.KEY_BOARD_DOWN.regionKey))
      image(skin.getDrawable(SkinImages.KEY_BOARD_UP.regionKey))
      label(this@ShopView.bundle["InventoryView.navigateInfo1"], SkinLabelStyle.DEFAULT.name)

      image(skin.getDrawable(SkinImages.GAME_PAD_A.regionKey)) { it.padLeft(20f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_SPACE.regionKey))
      label(this@ShopView.bundle["InventoryView.navigateInfo2"], SkinLabelStyle.DEFAULT.name)

      image(skin.getDrawable(SkinImages.GAME_PAD_B.regionKey)) { it.padLeft(20f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_ESCAPE.regionKey))
      label(this@ShopView.bundle["InventoryView.navigateInfo3"], SkinLabelStyle.DEFAULT.name)

      cell.expand().left()
        .colspan(2)
        .padLeft(12f).padBottom(6f)
    }

    // debugAll()
  }

  override fun onShow() {
    viewModel.addShopListener(this)

    with(goldLabel) {
      text.setLength(0)
      text.append(viewModel.gold())
      invalidateHierarchy()
    }
    bagItems.run {
      clearItems()
      viewModel.load()
    }
  }

  override fun onHide() {
    viewModel.removeShopListener(this)
  }

  override fun onSelectionChange(newIndex: Int, regionKey: String, description: String) {
    bagItems.selectedIndex = newIndex

    itemImage.drawable = skin.getDrawable(regionKey)
    itemImage.isVisible = regionKey != SkinImages.UNDEFINED.regionKey

    itemDescriptionLabel.setText(description)
  }

  /*override fun onBagUpdated(items: GdxArray<String>, selectionIndex: Int) {
    bagItems.run {
      clear()
      setItems(items)
      selectedIndex = selectionIndex
    }
  }*/

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.DOWN -> viewModel.moveItemSelectionIndex(1)
      Input.Keys.UP -> viewModel.moveItemSelectionIndex(-1)
      Input.Keys.SPACE -> viewModel.equipOrUseSelectedItem()
      Input.Keys.ESCAPE -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_DOWN -> viewModel.moveItemSelectionIndex(1)
      XboxInputProcessor.BUTTON_UP -> viewModel.moveItemSelectionIndex(-1)
      XboxInputProcessor.BUTTON_A -> viewModel.equipOrUseSelectedItem()
      XboxInputProcessor.BUTTON_B -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }
}
