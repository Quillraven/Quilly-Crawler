package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import ktx.actors.plusAssign

fun Label.addSelectionEffect() {
  this.clearActions()
  this += Actions.forever(Actions.sequence(Actions.fadeOut(0.5f), Actions.fadeIn(0.5f)))
}

fun Label.removeSelectionEffect() {
  this.clearActions()
  this.color.a = 1f
}
