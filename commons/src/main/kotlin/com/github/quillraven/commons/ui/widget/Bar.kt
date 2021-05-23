package com.github.quillraven.commons.ui.widget

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import ktx.actors.plusAssign
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

/**
 * Style for [Bar] widget
 */
data class BarStyle(
  val frame: Drawable,
  val bar: Drawable,
  var barOffsetX: Float,
  var barOffsetY: Float
)

/**
 * Alternative widget for [ProgressBar] but since that widget never delivers the result that I expect, I created
 * this widget instead.
 * It is a [WidgetGroup] that contains a background [BarStyle.frame] image and a [BarStyle.bar] image on top of it.
 * Additionally, you can define an offset for the [bar] in pixels to correctly place it within your [frame].
 * Use [BarStyle.barOffsetX] and [BarStyle.barOffsetY] for that.
 *
 * The [bar] image can be scaled horizontally by using the [fill] function.
 */
class Bar(
  styleName: String,
  skin: Skin,
) : WidgetGroup() {
  private val style: BarStyle = skin.get(styleName, BarStyle::class.java)
  private val frame: Image = Image(style.frame)
  private val bar: Image = Image(style.bar)

  init {
    width = frame.drawable.minWidth
    height = frame.drawable.minHeight
    bar.setPosition(style.barOffsetX, style.barOffsetY)

    addActor(frame)
    addActor(bar)
  }

  /**
   * Scales the [bar] image horizontally by the given [percentage] value over the [scaleDuration] in seconds.
   * The minimum value for [percentage] is 0 and the maximum is 1.
   */
  fun fill(percentage: Float, scaleDuration: Float = 0.75f) {
    bar.run {
      clearActions()
      this += scaleTo(MathUtils.clamp(percentage, 0f, 1f), 1f, scaleDuration)
    }
  }

  // for whatever reason we should not multiply here by scaleX because otherwise the widget gets a left indent
  override fun getPrefWidth() = width

  override fun getPrefHeight() = height * scaleY
}

/**
 * DSL extension function to use [Bar] together with LibKTX scene2d extensions
 */
@Scene2dDsl
inline fun <S> KWidget<S>.bar(
  style: String = ktx.scene2d.defaultStyle,
  skin: Skin = Scene2DSkin.defaultSkin,
  init: Bar.(S) -> Unit = {}
): Bar = actor(Bar(style, skin), init)

/**
 * Extension function to create a [BarStyle] instance for the given [frame] and [bar].
 * Adds the new instance to the [Skin] with the given [styleName].
 * Use [block] to modify the created [BarStyle] instance.
 */
fun Skin.bar(styleName: String, frame: Drawable, bar: Drawable, block: BarStyle.() -> Unit = {}) {
  this.add(styleName, BarStyle(frame, bar, 0f, 0f).apply(block))
}
