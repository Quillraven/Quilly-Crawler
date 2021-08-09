package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
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
import ktx.collections.isNotEmpty
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
  private val quillySplash: Image
  private val crawlerSplash: Image
  private val controlTable: Table
  private val deleteSaveTable: Table
  private val deleteYesBtn: TextButton
  private val deleteNoBtn: TextButton

  init {
    background = skin.getDrawable(SkinImages.FRAME_2.regionKey)
    defaults().width(45f)
      .pad(2f).padBottom(0f)

    // header
    textButton(bundle["StartUpView.title"], SkinTextButtonStyle.TITLE.name) { cell ->
      cell.top().padTop(5f).padBottom(6f)
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

    // controls table
    controlTable = Table(skin)
    controlTable.defaults().padRight(2f)
    controlTable.background = skin.getDrawable(SkinImages.FRAME_3.regionKey)
    controlTable.bottom().center()
    controlTable.setSize(280f, 25f)

    controlTable.add(Image(skin.getDrawable(SkinImages.GAME_PAD_DOWN.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.GAME_PAD_UP.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.KEY_BOARD_DOWN.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.KEY_BOARD_UP.regionKey)))
    controlTable.add(Label(bundle["StartUpView.navigateInfo1"], skin, SkinLabelStyle.DEFAULT.name))
      .padRight(10f)

    controlTable.add(Image(skin.getDrawable(SkinImages.GAME_PAD_LEFT.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.GAME_PAD_RIGHT.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.KEY_BOARD_LEFT.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.KEY_BOARD_RIGHT.regionKey)))
    controlTable.add(Label(bundle["StartUpView.navigateInfo3"], skin, SkinLabelStyle.DEFAULT.name))
      .padRight(10f)

    controlTable.add(Image(skin.getDrawable(SkinImages.GAME_PAD_A.regionKey)))
    controlTable.add(Image(skin.getDrawable(SkinImages.KEY_BOARD_SPACE.regionKey)))
    controlTable.add(Label(bundle["StartUpView.navigateInfo2"], skin, SkinLabelStyle.DEFAULT.name))

    // credits
    credits = Table(skin)
    credits.background = skin.getDrawable(SkinImages.WINDOW.regionKey)
    credits.setSize(230f, 50f)
    credits.defaults().fill().expand().top().padLeft(7f).padRight(7f)
    credits.add(Label(bundle["StartUpView.credits-info"], skin, SkinLabelStyle.LARGE.name).apply {
      wrap = true
    })

    // delete save state table
    deleteSaveTable = Table(skin)
    deleteSaveTable.background = skin.getDrawable(SkinImages.WINDOW.regionKey)
    deleteSaveTable.center()
    deleteSaveTable.setSize(width, 75f)
    deleteSaveTable.add(Label(bundle["StartUpView.delete-save"], skin, SkinLabelStyle.LARGE.name).apply {
      wrap = true
    }).expand().fill().colspan(2).padLeft(7f).row()
    deleteYesBtn = TextButton(bundle["YES"], skin, SkinTextButtonStyle.BRIGHT.name)
    deleteSaveTable.add(deleteYesBtn.apply {
      this.label.color = Color.BLACK
    }).pad(0f, 7f, 6f, 0f).left().expandX()
    deleteNoBtn = TextButton(bundle["NO"], skin, SkinTextButtonStyle.BRIGHT.name)
    deleteSaveTable.add(deleteNoBtn.apply {
      this.label.color = Color.BLACK
    }).pad(0f, 0f, 6f, 7f).right().expandX()

    // title splash art
    quillySplash = Image(skin.getDrawable(SkinImages.QUILLY.regionKey))
    crawlerSplash = Image(skin.getDrawable(SkinImages.CRAWLER.regionKey))

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
    centerPosition(width = stage.width * 0.9f)
    currentOption = OPT_NEW
    menuOptions[currentOption].label.addSelectionEffect()

    // add control info to stage
    stage.addActor(controlTable)
    controlTable.centerPosition(stage.width * 0.9f, controlTable.height)
    controlTable.y = 2f

    // add credits info to stage
    stage.addActor(credits)
    credits.centerPosition(width = stage.width * 0.9f)
    credits.alpha = 0f

    // delete save table
    stage.addActor(deleteSaveTable)
    deleteSaveTable.centerPosition(width = stage.width * 0.9f, stage.height + 25f)
    deleteSaveTable.isVisible = false

    // deactivate continue if there is no game state
    if (!viewModel.hasGameState()) {
      menuOptions[OPT_CONTINUE].isDisabled = true
    }

    menuOptions[OPT_MUSIC].label.setText("${(viewModel.musicVolume() * 100).roundToInt()}")
    menuOptions[OPT_SOUND].label.setText("${(viewModel.soundVolume() * 100).roundToInt()}")

    // add splash title art to stage
    stage.addActor(quillySplash)
    quillySplash.run {
      scaleBy(-0.5f)
      setPosition(10f, stage.height - quillySplash.height * this.scaleY - 35f)
      rotateBy(25f)
    }
    stage.addActor(crawlerSplash)
    crawlerSplash.run {
      scaleBy(-0.5f)
      setPosition(
        stage.width - crawlerSplash.width * this.scaleX + 2f,
        stage.height - crawlerSplash.height * this.scaleY
      )
      rotateBy(340f)
    }

    // fade in animation
    quillySplash.clearActions()
    quillySplash += sequence(Actions.moveBy(0f, 100f), moveBy(0f, -100f, 1.5f, Interpolation.bounce))
    crawlerSplash.clearActions()
    crawlerSplash += sequence(Actions.moveBy(0f, 100f), delay(0.25f), moveBy(0f, -100f, 1.5f, Interpolation.bounce))
    this.clearActions()
    this += sequence(alpha(0f), delay(4.5f), alpha(1f, 1.25f))
    controlTable.clearActions()
    controlTable += sequence(alpha(0f), delay(4.5f), alpha(1f, 1.25f))
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
    if (deleteSaveTable.isVisible) {
      // delete save popup visible -> do not change menu options
      return
    }

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
      OPT_NEW -> {
        if (deleteSaveTable.isVisible) {
          if (deleteYesBtn.label.actions.isNotEmpty()) {
            // yes selection
            viewModel.newGame()
          } else {
            // no selection -> hide popup again
            deleteSaveTable.isVisible = false
            viewModel.changeOption()
          }
        } else if (viewModel.hasGameState()) {
          // ask user if he really wants to delete his previous state
          deleteSaveTable.isVisible = true
          deleteYesBtn.label.removeSelectionEffect()
          deleteNoBtn.label.addSelectionEffect()
        } else {
          viewModel.newGame()
        }
      }
      OPT_CONTINUE -> viewModel.continueGame()
      OPT_CREDITS -> {
        viewModel.changeOption()
        credits.clearActions()
        credits += alpha(1f, 0.5f)
      }
      OPT_QUIT -> viewModel.quitGame()
    }
  }

  private fun changeYesNo() {
    if (deleteYesBtn.label.actions.isNotEmpty()) {
      // yes button currently selected -> switch to no
      deleteYesBtn.label.removeSelectionEffect()
      deleteNoBtn.label.addSelectionEffect()
    } else {
      // no btn selected -> switch to yes
      deleteNoBtn.label.removeSelectionEffect()
      deleteYesBtn.label.addSelectionEffect()
    }
    viewModel.changeOption()
  }

  override fun keyDown(keycode: Int): Boolean {
    if (this.alpha < 1f) {
      // menu not active yet and still fading in -> do nothing
      return true
    }

    if (credits.alpha > 0f) {
      credits.clearActions()
      credits += alpha(0f, 0.3f)
    }

    when (keycode) {
      Input.Keys.DOWN -> moveOption(1)
      Input.Keys.UP -> moveOption(-1)
      Input.Keys.LEFT -> {
        if (deleteSaveTable.isVisible) {
          changeYesNo()
        } else {
          when (currentOption) {
            OPT_MUSIC -> changeMusicVolume(-0.05f)
            OPT_SOUND -> changeSoundVolume(-0.05f)
          }
        }
      }
      Input.Keys.RIGHT -> {
        if (deleteSaveTable.isVisible) {
          changeYesNo()
        } else {
          when (currentOption) {
            OPT_MUSIC -> changeMusicVolume(0.05f)
            OPT_SOUND -> changeSoundVolume(0.05f)
          }
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
