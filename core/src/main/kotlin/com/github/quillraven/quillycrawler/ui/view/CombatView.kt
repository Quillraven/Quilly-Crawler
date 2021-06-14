package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.commons.ui.widget.Bar
import com.github.quillraven.commons.ui.widget.bar
import com.github.quillraven.quillycrawler.ui.*
import com.github.quillraven.quillycrawler.ui.model.CombatUiListener
import com.github.quillraven.quillycrawler.ui.model.CombatViewModel
import com.github.quillraven.quillycrawler.ui.model.ItemViewModel
import ktx.actors.alpha
import ktx.actors.centerPosition
import ktx.actors.plus
import ktx.actors.plusAssign
import ktx.collections.GdxArray
import ktx.collections.iterate
import ktx.scene2d.*
import kotlin.math.roundToInt
import com.badlogic.gdx.scenes.scene2d.ui.List as GdxList

class CombatView(
  private val viewModel: CombatViewModel,
  private val bundle: I18NBundle = viewModel.bundle
) : View(), CombatUiListener {
  private val turnLabel: Label
  private val turnOrderTable: Table
  private val turnLabelText = bundle["CombatView.turn"]
  private val orderButtons = GdxArray<TextButton>()
  private var activeOrderIdx = IDX_ATTACK
  private val abilityList: GdxList<String>
  private val itemList: GdxList<ItemViewModel>
  private val abilityItemTable: Table
  private val lifeLabel: Label
  private val lifeBar: Bar
  private val manaLabel: Label
  private val manaBar: Bar
  private val selection = Image(skin.getDrawable(SkinImages.SELECTION.regionKey))
  private val selectionTargets = GdxArray<Vector2>()
  private var currentSelectionTarget = -1
  private var waitForTurn = true
  private val btnDefeat = TextButton(bundle["CombatView.defeat"], skin, SkinTextButtonStyle.RED.name)
  private val btnVictory = TextButton(bundle["CombatView.victory"], skin, SkinTextButtonStyle.GREEN.name)
  private val playerBuffs = GdxArray<Image>()

  init {
    setFillParent(true)

    //
    // Turn Label
    //
    turnLabel = label(turnLabelText, SkinLabelStyle.FRAMED_BRIGHT.name) { cell ->
      cell.left().top()
        .expand().height(17f)
        .padTop(3f).padLeft(3f)
        .colspan(2)
    }

    //
    // Table that shows the order of entities for the next round
    //
    // !!! Make sure that this is the last child to be rendered because it is using
    // !!! the normal entity atlas instead of the UI atlas to render the entity's textures.
    // !!! If it is not the last child then there are additional texture bindings
    // !!! refer to the call below 'sp.zIndex = children.size - 1
    turnOrderTable = Table(skin).apply { top() }
    val sp = scrollPane(SkinScrollPaneStyle.DEFAULT.name) { cell ->
      setScrollingDisabled(true, false)
      setScrollbarsVisible(false)

      actor = this@CombatView.turnOrderTable

      cell.top().right()
        .width(35f).height(85f)
        .padRight(3f).padTop(3f)
        .row()
    }

    //
    // Command Buttons for attack, abilities and items
    //
    table { cell ->
      defaults().left().padBottom(2f)

      textButton(
        "$TXT_COLOR_BLACK${this@CombatView.bundle["CombatView.attack"]}[]",
        SkinTextButtonStyle.BRIGHT.name
      ) { it.row() }
      textButton(
        "$TXT_COLOR_BLACK${this@CombatView.bundle["CombatView.ability"]}[]",
        SkinTextButtonStyle.BRIGHT.name
      ) { it.row() }
      textButton("$TXT_COLOR_BLACK${this@CombatView.bundle["CombatView.item"]}[]", SkinTextButtonStyle.BRIGHT.name)
      children.forEach { btn -> this@CombatView.orderButtons.add(btn as TextButton) }

      cell.bottom().left().padLeft(3f).padBottom(1f).bottom()
    }

    //
    // Ability / Item selection
    //
    abilityItemTable = table { cell ->
      background = skin.getDrawable(SkinImages.BUTTON_2.regionKey)
      defaults().expand().fill()

      stack {
        this@CombatView.abilityList = listWidget(SkinListStyle.LARGE.name)
        this@CombatView.itemList = listWidget(SkinListStyle.LARGE.name)
      }

      cell.expand().left().bottom()
        .padBottom(3f)
        .padLeft(15f)
        .width(80f).height(50f)

      isVisible = false
    }

    //
    // Life / Mana bars of player
    table { cell ->
      this@CombatView.lifeLabel = label(" / ", SkinLabelStyle.LARGE.name) { it.padLeft(45f).height(7f).row() }
      this@CombatView.lifeBar = bar(SkinBarStyle.LIFE.name) {
        setScale(1.5f)
        it.padBottom(3f).padLeft(40f).row()
      }

      this@CombatView.manaLabel = label(" / ", SkinLabelStyle.LARGE.name) { it.padLeft(45f).height(7f).row() }
      this@CombatView.manaBar = bar(SkinBarStyle.MANA.name) {
        setScale(1.25f, 1.5f)
        it.padBottom(7f).padLeft(40f)
      }

      cell.expand().bottom().left().padBottom(3f)
    }

    // set scroll pane of entity order as last child to be rendered to avoid additional texture bindings
    sp.zIndex = children.size - 1

    // debugAll()
  }

  private fun selectButton(idx: Int) {
    // move previous button to origin position
    val oldBtn = orderButtons[activeOrderIdx]
    oldBtn.clearActions()
    oldBtn += moveTo(0f, oldBtn.y, 0.25f)
    oldBtn.label.text.replace(TXT_COLOR_WHITE, TXT_COLOR_BLACK)
    oldBtn.label.invalidateHierarchy()

    // update index
    activeOrderIdx = when {
      idx < 0 -> orderButtons.size - 1
      idx >= orderButtons.size -> 0
      else -> idx
    }

    // move new button a little bit to the right
    val newBtn = orderButtons[activeOrderIdx]
    newBtn.clearActions()
    newBtn += moveTo(5f, newBtn.y, 0.25f)
    newBtn.label.text.replace(TXT_COLOR_BLACK, TXT_COLOR_WHITE)
    newBtn.label.invalidateHierarchy()
  }

  private fun selectTarget(idx: Int) {
    if (!selection.isVisible) {
      return
    }

    currentSelectionTarget = when {
      idx < 0 -> selectionTargets.size - 1
      idx >= selectionTargets.size -> 0
      else -> idx
    }

    selection.setPosition(
      selectionTargets[currentSelectionTarget].x - selection.imageWidth * 0.5f,
      selectionTargets[currentSelectionTarget].y - 15f
    )

    viewModel.selectTarget(selectionTargets[currentSelectionTarget])
  }

  override fun onShow() {
    stage.addActor(selection)
    stage.addActor(btnDefeat)
    stage.addActor(btnVictory)
    selection.isVisible = false
    abilityList.isVisible = false
    itemList.isVisible = false
    btnDefeat.isVisible = false
    btnDefeat.centerPosition()
    btnVictory.isVisible = false
    btnVictory.centerPosition()

    viewModel.addCombatListener(this)

    orderButtons.forEach {
      it.label.text.replace(TXT_COLOR_WHITE, TXT_COLOR_BLACK)
      it.label.invalidateHierarchy()
    }
    activeOrderIdx = IDX_ATTACK
    selectButton(activeOrderIdx)
  }

  override fun onHide() {
    viewModel.removeCombatListener(this)

    // Clear any order children since they are images and will link to a null texture which causes a crash.
    // The reason is that we pool those images to avoid recreating them and during reset the texture gets set to null.
    turnOrderTable.clear()
  }

  private fun updateLifeInfo(life: Float, maxLife: Float, scaleDuration: Float = 0.5f) {
    lifeBar.fill(life / maxLife, scaleDuration)
    lifeLabel.text.clear()
    lifeLabel.text.append(life.roundToInt()).append(" / ").append(maxLife.roundToInt())
    lifeLabel.invalidateHierarchy()
  }

  private fun updateManaInfo(mana: Float, maxMana: Float, scaleDuration: Float = 0.5f) {
    manaBar.fill(mana / maxMana, scaleDuration)
    manaLabel.text.clear()
    manaLabel.text.append(mana.roundToInt()).append(" / ").append(maxMana.roundToInt())
    manaLabel.invalidateHierarchy()
  }

  override fun onCombatStart(life: Float, maxLife: Float, mana: Float, maxMana: Float) {
    updateLifeInfo(life, maxLife, 0f)
    updateManaInfo(mana, maxMana, 0f)
  }

  override fun onDefeat() {
    btnDefeat.isVisible = true
    btnDefeat.clearActions()
    btnDefeat += alpha(0f) + fadeIn(1.5f)
  }

  override fun onVictory() {
    btnVictory.isVisible = true
    btnVictory.clearActions()
    btnVictory += alpha(0f) + fadeIn(1.5f)
  }

  override fun onNextTurn(
    turn: Int,
    entityImages: GdxArray<Image>,
    abilities: GdxArray<String>,
    items: GdxArray<ItemViewModel>,
    targets: GdxArray<Vector2>
  ) {
    // activate player input
    waitForTurn = false

    // update turn label
    with(turnLabel.text) {
      setLength(turnLabelText.length)
      append(" ").append(turn)
      turnLabel.invalidateHierarchy()
    }

    // update turn order entity images
    turnOrderTable.clear()
    for (i in 0 until entityImages.size) {
      turnOrderTable.add(entityImages[entityImages.size - 1 - i].apply { layout() }).height(16f).row()
    }

    // update abilities and items
    abilityList.run {
      setItems(abilities)
      selectAbility(0)
    }
    itemList.run {
      setItems(items)
      selectedIndex = if (items.isEmpty) -1 else 0
    }

    // update selection targets
    selectionTargets.clear()
    selectionTargets.addAll(targets)
    selectTarget(0)
    currentSelectionTarget = 0
  }

  override fun onLifeChange(life: Float, maxLife: Float) = updateLifeInfo(life, maxLife)

  override fun onManaChange(mana: Float, maxMana: Float) = updateManaInfo(mana, maxMana)

  private fun newFloatingText(text: String, position: Vector2) {
    val dmgLabel = Label(text, skin, SkinLabelStyle.LARGE.name)
    stage.addActor(dmgLabel)
    dmgLabel.setPosition(position.x, position.y)
    dmgLabel += parallel(
      moveBy(
        MathUtils.random(-stage.width * 0.1f, stage.width * 0.1f),
        stage.height * 0.15f,
        2.5f,
        Interpolation.circleOut
      ), fadeOut(2.5f)
    ) + after(Actions.removeActor(dmgLabel))
  }

  override fun onDamage(entityPos: Vector2, damage: Float) {
    newFloatingText("[#ff0000]${damage.roundToInt()}[]", entityPos)
  }

  override fun onHeal(entityPos: Vector2, life: Float, mana: Float) {
    if (life > 0f) {
      newFloatingText("[#00ff00]+${life.roundToInt()}[]", entityPos)
    }
    if (mana != 0f) {
      newFloatingText("[#0000ff]${if (mana > 0f) "+" else ""}${mana.roundToInt()}[]", entityPos)
    }
  }

  override fun onBuffsUpdated(entityPos: Vector2, buffRegionKeys: GdxArray<String>) {
    playerBuffs.iterate { image, iterator ->
      image += fadeOut(0.5f, Interpolation.bounceOut) + Actions.removeActor(image)
      iterator.remove()
    }

    var imgIdx = 0
    buffRegionKeys.forEach { regionKey ->
      val image = Image(skin.getDrawable(regionKey))
      image.scaleBy(-0.25f)
      image.alpha = 0f
      image += fadeIn(0.5f, Interpolation.bounceIn)
      stage.addActor(image)
      // every image is 16 pixels -> move every image 16 pixels times scaling to the right
      image.setPosition(entityPos.x + (imgIdx * 16 * 0.75f), entityPos.y)
      playerBuffs.add(image)
      ++imgIdx
    }
  }

  private fun hasSelectableAbility(): Boolean {
    if (abilityList.items.isEmpty) {
      return false
    }

    var numDisabled = 0
    abilityList.items.forEach {
      if (it.startsWith(CombatViewModel.DISABLED_COMMAND)) {
        ++numDisabled
      }
    }

    return numDisabled < abilityList.items.size
  }

  private fun goToNextValidAbility(direction: Int) {
    val origIdx = abilityList.selectedIndex

    abilityList.selectedIndex = when {
      abilityList.selectedIndex + direction < 0 -> abilityList.items.size - 1
      abilityList.selectedIndex + direction >= abilityList.items.size -> 0
      else -> abilityList.selectedIndex + direction
    }

    while (origIdx != abilityList.selectedIndex) {
      abilityList.selectedIndex = when {
        abilityList.selectedIndex < 0 -> abilityList.items.size - 1
        abilityList.selectedIndex >= abilityList.items.size -> 0
        else -> abilityList.selectedIndex
      }

      if (!abilityList.selected.startsWith(CombatViewModel.DISABLED_COMMAND)) {
        return
      }

      abilityList.selectedIndex += direction
    }
  }

  private fun selectAbility(idx: Int) {
    if (!hasSelectableAbility()) {
      abilityList.selectedIndex = -1
      return
    }

    val direction = idx - abilityList.selectedIndex
    abilityList.selectedIndex = when {
      idx < 0 -> abilityList.items.size - 1
      idx >= abilityList.items.size -> 0
      else -> idx
    }

    if (abilityList.selected.startsWith(CombatViewModel.DISABLED_COMMAND)) {
      // ability cannot be casted -> select next castable ability
      goToNextValidAbility(if (direction == 0) 1 else direction)
    }

    viewModel.selectCommand(abilityList.selected)
  }

  private fun selectItem(idx: Int) {
    if (itemList.items.isEmpty) {
      return
    }

    itemList.selectedIndex = when {
      idx < 0 -> itemList.items.size - 1
      idx >= itemList.items.size -> 0
      else -> idx
    }
    viewModel.selectItem(itemList.selected.itemName)
  }

  private fun navigateUp() {
    if (btnVictory.isVisible || btnDefeat.isVisible) {
      return
    }

    when {
      selection.isVisible -> return
      abilityList.isVisible -> selectAbility(abilityList.selectedIndex - 1)
      itemList.isVisible -> selectItem(itemList.selectedIndex - 1)
      else -> selectButton(activeOrderIdx - 1)
    }
  }

  private fun navigateDown() {
    if (btnVictory.isVisible || btnDefeat.isVisible) {
      return
    }

    when {
      selection.isVisible -> return
      abilityList.isVisible -> selectAbility(abilityList.selectedIndex + 1)
      itemList.isVisible -> selectItem(itemList.selectedIndex + 1)
      else -> selectButton(activeOrderIdx + 1)
    }
  }

  private fun executeSelection() {
    waitForTurn = true
    selection.isVisible = false
    abilityItemTable.isVisible = false
    abilityList.isVisible = false
    itemList.isVisible = false
    viewModel.executeOrder()
  }

  private fun navigateBackwards() {
    if (btnVictory.isVisible || btnDefeat.isVisible) {
      return
    }

    when {
      selection.isVisible -> {
        selection.isVisible = false
        selectTarget(0)
      }
      abilityList.isVisible -> {
        selectAbility(0)
        abilityList.isVisible = false
        abilityItemTable.isVisible = false
      }
      itemList.isVisible -> {
        selectItem(0)
        itemList.isVisible = false
        abilityItemTable.isVisible = false
      }
    }
  }

  private fun doSelection() {
    when {
      btnVictory.isVisible || btnDefeat.isVisible -> viewModel.returnToGame()
      selection.isVisible -> executeSelection()
      abilityList.isVisible -> {
        when {
          abilityList.selectedIndex == -1 -> {
            // there are no valid abilities to cast (e.g. insufficient mana)
            return
          }
          viewModel.isSingleTargetCommand() -> {
            // single target command -> choose target
            selection.isVisible = true
            selectTarget(0)
          }
          else -> {
            // no target or all target command -> execute it
            executeSelection()
          }
        }
      }
      itemList.isVisible -> {
        if (itemList.selectedIndex == -1) {
          // no valid item selected -> do nothing
          return
        } else {
          executeSelection()
        }
      }
      else -> {
        when (activeOrderIdx) {
          IDX_ABILITY -> {
            // ability command selection
            abilityItemTable.isVisible = true
            abilityList.isVisible = true
            itemList.isVisible = false
            selectAbility(0)
          }
          IDX_ITEM -> {
            // item command selection
            abilityItemTable.isVisible = true
            abilityList.isVisible = false
            itemList.isVisible = true
            selectItem(0)
            viewModel.selectItemCommand()
          }
          else -> {
            // attack selection
            abilityItemTable.isVisible = false
            abilityList.isVisible = false
            itemList.isVisible = false
            selection.isVisible = true
            selectTarget(0)
            viewModel.selectAttackCommand()
          }
        }
      }
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    if (waitForTurn) {
      return false
    }

    when (keycode) {
      Input.Keys.ESCAPE -> navigateBackwards()
      Input.Keys.UP -> navigateUp()
      Input.Keys.DOWN -> navigateDown()
      Input.Keys.LEFT -> selectTarget(currentSelectionTarget - 1)
      Input.Keys.RIGHT -> selectTarget(currentSelectionTarget + 1)
      Input.Keys.SPACE -> doSelection()
      else -> return false
    }

    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    if (waitForTurn) {
      return false
    }

    when (buttonCode) {
      XboxInputProcessor.BUTTON_B -> navigateBackwards()
      XboxInputProcessor.BUTTON_UP -> navigateUp()
      XboxInputProcessor.BUTTON_DOWN -> navigateDown()
      XboxInputProcessor.BUTTON_LEFT -> selectTarget(currentSelectionTarget - 1)
      XboxInputProcessor.BUTTON_RIGHT -> selectTarget(currentSelectionTarget + 1)
      XboxInputProcessor.BUTTON_A -> doSelection()
      else -> return false
    }

    return true
  }

  companion object {
    private const val IDX_ATTACK = 0
    private const val IDX_ABILITY = 1
    private const val IDX_ITEM = 2
    private const val TXT_COLOR_WHITE = "[#ffffff]"
    private const val TXT_COLOR_BLACK = "[#000000]"
  }
}
