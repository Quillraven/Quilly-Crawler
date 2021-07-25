package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.StartUpViewModel
import ktx.actors.alpha
import ktx.actors.centerPosition
import ktx.actors.plus
import ktx.actors.plusAssign
import ktx.collections.GdxArray
import ktx.scene2d.*
import kotlin.math.roundToInt

class StartUpView(
  private val viewModel: StartUpViewModel,
  private val bundle: I18NBundle = viewModel.bundle
) : View() {
  private val menuOptions = GdxArray<TextButton>()
  private var currentOption = OPT_NEW
  private val musicDwn: Image
  private val musicUp: Image
  private val soundDown: Image
  private val soundUp: Image
  private val credits: Table

  init {
    background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
    defaults().width(45f)
      .pad(2f)

    // header
    textButton(bundle["StartUpView.title"], SkinTextButtonStyle.TITLE.name) { cell ->
      cell.top().padTop(5f).padBottom(15f)
        .height(25f).width(110f)
        .colspan(2)
        .row()
    }

    // menu options
    textButton(bundle["StartUpView.new-game"], SkinTextButtonStyle.DEFAULT.name) { c ->
      this@StartUpView.menuOptions.add(this)
      c.row()
    }
    textButton(bundle["StartUpView.continue"], SkinTextButtonStyle.DEFAULT.name) { c ->
      this@StartUpView.menuOptions.add(this)
      c.row()
    }
    table { tableCell ->
      this@StartUpView.musicDwn = image(skin.getDrawable(SkinImages.KEY_BOARD_LEFT.regionKey)) { c ->
        c.bottom()
      }
      verticalGroup {
        label(this@StartUpView.bundle["StartUpView.music-volume"], SkinLabelStyle.DEFAULT.name)
        textButton("100", SkinTextButtonStyle.DEFAULT.name) { this@StartUpView.menuOptions.add(this) }
      }
      this@StartUpView.musicUp = image(skin.getDrawable(SkinImages.KEY_BOARD_RIGHT.regionKey)) { c ->
        c.bottom()
      }

      tableCell.width(110f).row()
    }
    table { tableCell ->
      this@StartUpView.soundDown = image(skin.getDrawable(SkinImages.KEY_BOARD_LEFT.regionKey)) { c ->
        c.bottom()
      }
      verticalGroup {
        label(this@StartUpView.bundle["StartUpView.sound-volume"], SkinLabelStyle.DEFAULT.name)
        textButton("100", SkinTextButtonStyle.DEFAULT.name) { this@StartUpView.menuOptions.add(this) }
      }
      this@StartUpView.soundUp = image(skin.getDrawable(SkinImages.KEY_BOARD_RIGHT.regionKey)) { c ->
        c.bottom()
      }

      tableCell.width(110f).row()
    }
    textButton(bundle["StartUpView.credits"], SkinTextButtonStyle.DEFAULT.name) { c ->
      this@StartUpView.menuOptions.add(this)
      c.row()
    }
    textButton(bundle["StartUpView.quit"], SkinTextButtonStyle.DEFAULT.name) { c ->
      this@StartUpView.menuOptions.add(this)
      c.row()
    }

    // credits
    credits = Table(skin)
    credits.background = skin.getDrawable(SkinImages.WINDOW.regionKey)
    credits.setSize(230f, 50f)
    credits.defaults().fill().expand().top().padLeft(7f).padRight(7f)
    credits.add(Label(bundle["StartUpView.credits-info"], skin, SkinLabelStyle.LARGE.name).apply {
      wrap = true
    })

    top()

    // debugAll()
  }

  override fun getWidth(): Float {
    return 130f
  }

  override fun getHeight(): Float {
    return 170f
  }

  override fun onShow() {
    centerPosition()
    currentOption = OPT_NEW
    menuOptions[currentOption].label.addSelectionEffect()

    if (!viewModel.hasGameState()) {
      menuOptions[OPT_CONTINUE].isDisabled = true
    }

    stage.addActor(credits)
    credits.centerPosition()
    credits.alpha = 0f
  }

  override fun onHide() {
  }

  private fun nextOptionIdx(direction: Int): Int {
    return when {
      currentOption + direction < 0 -> menuOptions.size - 1
      currentOption + direction >= menuOptions.size -> 0
      else -> currentOption + direction
    }
  }

  private fun moveOption(direction: Int) {
    menuOptions[currentOption].label.removeSelectionEffect()

    currentOption = nextOptionIdx(direction)
    while (menuOptions[currentOption].isDisabled) {
      currentOption = nextOptionIdx(direction)
    }

    menuOptions[currentOption].label.addSelectionEffect()
    viewModel.changeOption()
  }

  private fun changeMusicVolume(amount: Float) {
    val newValue = viewModel.changeMusicVolume(amount)
    menuOptions[OPT_MUSIC].label.setText("${(newValue * 100).roundToInt()}")
    if (amount > 0f) {
      musicUp.clearActions()
      musicUp += alpha(1f) + alpha(0.5f, 0.2f) + alpha(1f, 0.1f)
    } else {
      musicDwn.clearActions()
      musicDwn += alpha(1f) + alpha(0.5f, 0.2f) + alpha(1f, 0.1f)
    }
  }

  private fun changeSoundVolume(amount: Float) {
    val newValue = viewModel.changeSoundVolume(amount)
    menuOptions[OPT_SOUND].label.setText("${(newValue * 100).roundToInt()}")
    if (amount > 0f) {
      soundUp.clearActions()
      soundUp += alpha(1f) + alpha(0.5f, 0.2f) + alpha(1f, 0.1f)
    } else {
      soundDown.clearActions()
      soundDown += alpha(1f) + alpha(0.5f, 0.2f) + alpha(1f, 0.1f)
    }
  }

  private fun selectCurrentOption() {
    when (currentOption) {
      OPT_NEW -> viewModel.newGame()
      OPT_CONTINUE -> viewModel.continueGame()
      OPT_CREDITS -> {
        viewModel.changeOption()
        credits.clearActions()
        credits += alpha(1f, 0.5f)
      }
      OPT_QUIT -> viewModel.quitGame()
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    if (credits.alpha > 0f) {
      credits.clearActions()
      credits += alpha(0f, 0.3f)
    }

    when (keycode) {
      Input.Keys.DOWN -> moveOption(1)
      Input.Keys.UP -> moveOption(-1)
      Input.Keys.LEFT -> {
        when (currentOption) {
          OPT_MUSIC -> changeMusicVolume(-0.05f)
          OPT_SOUND -> changeSoundVolume(-0.05f)
        }
      }
      Input.Keys.RIGHT -> {
        when (currentOption) {
          OPT_MUSIC -> changeMusicVolume(0.05f)
          OPT_SOUND -> changeSoundVolume(0.05f)
        }
      }
      Input.Keys.SPACE -> {
        selectCurrentOption()
      }
    }
    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_DOWN -> keyDown(Input.Keys.DOWN)
      XboxInputProcessor.BUTTON_UP -> keyDown(Input.Keys.UP)
      XboxInputProcessor.BUTTON_LEFT -> keyDown(Input.Keys.LEFT)
      XboxInputProcessor.BUTTON_RIGHT -> keyDown(Input.Keys.RIGHT)
      XboxInputProcessor.BUTTON_A -> keyDown(Input.Keys.SPACE)
    }

    return true
  }

  companion object {
    private const val OPT_NEW = 0
    private const val OPT_CONTINUE = 1
    private const val OPT_MUSIC = 2
    private const val OPT_SOUND = 3
    private const val OPT_CREDITS = 4
    private const val OPT_QUIT = 5
  }
}
