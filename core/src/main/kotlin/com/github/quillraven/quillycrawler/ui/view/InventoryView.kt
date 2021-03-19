package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ashley.component.GearType
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinListStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.InventoryViewModel
import ktx.scene2d.*
import java.util.*
import com.badlogic.gdx.scenes.scene2d.ui.List as GdxList

class InventoryView(private val viewModel: InventoryViewModel, private val bundle: I18NBundle) :
  Table(Scene2DSkin.defaultSkin), KTable,
  InputProcessor, XboxInputProcessor {
  // item details
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
      this.labelCell.padTop(4f)
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

      this@InventoryView.bagItems =
        listWidget<String>(SkinListStyle.DEFAULT.name).cell(padTop = 3f, padLeft = 2f, padRight = 2f)

      cell.expand()
        .padBottom(3f)
        .width(95f).height(120f)
    }

    // item details and stats table
    table { tableCell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

      // item details
      this@InventoryView.itemImage = image(skin.getDrawable(SkinImages.UNDEFINED.regionKey)) { cell ->
        setScaling(Scaling.fit)
        cell.top().padLeft(4f).padTop(6f).minWidth(20f)
      }
      this@InventoryView.itemDescriptionLabel = label("", SkinLabelStyle.DEFAULT.name) { cell ->
        setAlignment(Align.left)
        wrap = true
        cell.expandX().fill()
          .padLeft(4f).padRight(4f).padTop(6f)
          .height(23f)
          .row()
      }

      // gear
      table { gearTableCell ->
        background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
        defaults().width(40f).expandX().fill().left().padTop(2f)

        this@InventoryView.helmetLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@InventoryView.amuletLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@InventoryView.armorLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@InventoryView.shieldLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@InventoryView.bootsLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@InventoryView.glovesLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@InventoryView.weaponLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }

        gearTableCell.expand().fill().colspan(2)
          .padTop(2f)
          .width(176f).height(34f)
          .row()
      }

      // stats
      table { statsTableCell ->
        background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
        defaults().width(86f).fill().left().padTop(3f)

        this@InventoryView.lifeLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f).padTop(5f) }
        this@InventoryView.physDamageLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padTop(5f).row() }
        this@InventoryView.manaLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@InventoryView.magicDamageLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }
        this@InventoryView.physArmorLabel = label("", SkinLabelStyle.DEFAULT.name) { it.padLeft(8f) }
        this@InventoryView.magicArmorLabel = label("", SkinLabelStyle.DEFAULT.name) { it.row() }

        this@InventoryView.strengthLabel = label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).row() }
        this@InventoryView.intelligenceLabel =
          label("", SkinLabelStyle.DEFAULT.name) { it.colspan(2).padLeft(8f).row() }
        this@InventoryView.agilityLabel =
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
      label(this@InventoryView.bundle["InventoryView.navigateInfo1"], SkinLabelStyle.DEFAULT.name) {
        it.padTop(4f)
      }

      image(skin.getDrawable(SkinImages.GAME_PAD_A.regionKey)) { it.padLeft(20f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_SPACE.regionKey))
      label(this@InventoryView.bundle["InventoryView.navigateInfo2"], SkinLabelStyle.DEFAULT.name) {
        it.padTop(4f)
      }

      image(skin.getDrawable(SkinImages.GAME_PAD_B.regionKey)) { it.padLeft(20f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_ESCAPE.regionKey))
      label(this@InventoryView.bundle["InventoryView.navigateInfo3"], SkinLabelStyle.DEFAULT.name) {
        it.padTop(4f)
      }

      cell.expand().left()
        .colspan(2)
        .padLeft(12f).padBottom(6f)
    }

    // debugAll()
  }

  override fun setStage(stage: Stage?) {
    if (stage == null) {
      // active screen changes away from InventoryScreen
      removeXboxControllerListener()
      Gdx.input.inputProcessor = null
    } else {
      // InventoryScreen becomes active screen
      addXboxControllerListener()
      Gdx.input.inputProcessor = this

      bagItems.run {
        clearItems()
        viewModel.load { items, firstItemIndex, regionKey, description ->
          setItems(items)
          onSelectionChange(firstItemIndex, regionKey, description)
        }
      }
    }
    super.setStage(stage)
  }

  private fun onSelectionChange(newIndex: Int, regionKey: String, description: String) {
    bagItems.selectedIndex = newIndex

    itemImage.drawable = skin.getDrawable(regionKey)
    itemImage.isVisible = regionKey != SkinImages.UNDEFINED.regionKey

    itemDescriptionLabel.setText(description)

    viewModel.statsAndGearInfo(::onEquipOrUseItem)
  }

  private fun onEquipOrUseItem(
    statsInfo: EnumMap<StatsType, StringBuilder>,
    gearInfo: EnumMap<GearType, StringBuilder>
  ) {
    // stats
    lifeLabel.setText(statsInfo[StatsType.LIFE])
    manaLabel.setText(statsInfo[StatsType.MANA])
    strengthLabel.setText(statsInfo[StatsType.STRENGTH])
    agilityLabel.setText(statsInfo[StatsType.AGILITY])
    intelligenceLabel.setText(statsInfo[StatsType.INTELLIGENCE])
    physDamageLabel.setText(statsInfo[StatsType.PHYSICAL_DAMAGE])
    physArmorLabel.setText(statsInfo[StatsType.PHYSICAL_ARMOR])
    magicDamageLabel.setText(statsInfo[StatsType.MAGIC_DAMAGE])
    magicArmorLabel.setText(statsInfo[StatsType.MAGIC_ARMOR])

    // gear
    helmetLabel.setText(gearInfo[GearType.HELMET])
    amuletLabel.setText(gearInfo[GearType.AMULET])
    armorLabel.setText(gearInfo[GearType.ARMOR])
    weaponLabel.setText(gearInfo[GearType.WEAPON])
    shieldLabel.setText(gearInfo[GearType.SHIELD])
    bootsLabel.setText(gearInfo[GearType.BOOTS])
    glovesLabel.setText(gearInfo[GearType.GLOVES])
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.DOWN -> viewModel.moveItemSelectionIndex(1, ::onSelectionChange)
      Input.Keys.UP -> viewModel.moveItemSelectionIndex(-1, ::onSelectionChange)
      Input.Keys.SPACE -> viewModel.equipOrUseSelectedItem(::onEquipOrUseItem)
      Input.Keys.ESCAPE -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }

  override fun keyUp(keycode: Int) = false

  override fun keyTyped(character: Char) = false

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false

  override fun mouseMoved(screenX: Int, screenY: Int) = false

  override fun scrolled(amountX: Float, amountY: Float) = false

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_DOWN -> viewModel.moveItemSelectionIndex(1, ::onSelectionChange)
      XboxInputProcessor.BUTTON_UP -> viewModel.moveItemSelectionIndex(-1, ::onSelectionChange)
      XboxInputProcessor.BUTTON_A -> viewModel.equipOrUseSelectedItem(::onEquipOrUseItem)
      XboxInputProcessor.BUTTON_B -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }

  override fun buttonUp(controller: Controller?, buttonCode: Int) = false

  override fun axisMoved(controller: Controller?, axisCode: Int, value: Float) = false
}
