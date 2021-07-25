package com.github.quillraven.quillycrawler.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.github.quillraven.commons.ui.widget.bar
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.scene2d.Scene2DSkin
import ktx.style.*

enum class SkinImages(val regionKey: String) {
  UNDEFINED("undefined"),
  BAR_FRAME("bar_frame-1"),
  BAR_RED("bar_red"),
  BAR_BLUE("bar_blue"),
  BUTTON_1("button-1"),
  BUTTON_2("button-2"),
  WINDOW("window-1"),
  FRAME_1("frame-1"),
  FRAME_2("frame-2"),
  FRAME_3("frame-3"),
  FRAME_RED("frame-red"),
  FRAME_GREEN("frame-green"),
  GAME_PAD_DOWN("gamepad-down"),
  GAME_PAD_UP("gamepad-up"),
  GAME_PAD_LEFT("gamepad-left"),
  GAME_PAD_RIGHT("gamepad-right"),
  GAME_PAD_A("gamepad-a"),
  GAME_PAD_B("gamepad-b"),
  KEY_BOARD_SPACE("button-space"),
  KEY_BOARD_UP("button-up"),
  KEY_BOARD_DOWN("button-down"),
  KEY_BOARD_LEFT("button-left"),
  KEY_BOARD_RIGHT("button-right"),
  KEY_BOARD_ESCAPE("button-esc"),
  SELECTION("selection"),
  GOLD("gold"),
}

enum class SkinFontStyle(val fntFilePath: String, val regionKey: String, val scale: Float) {
  DEFAULT("fonts/immortal.fnt", "immortal", 0.15f),
  LARGE("fonts/immortal.fnt", "immortal", 0.275f),
  TITLE("fonts/immortal.fnt", "immortal", 0.4f),
}

enum class SkinTextButtonStyle {
  TITLE, DEFAULT, BRIGHT, GREEN, RED
}

enum class SkinLabelStyle {
  DEFAULT, LARGE, DUNGEON_LEVEL, FRAMED_BRIGHT
}

enum class SkinListStyle {
  DEFAULT, LARGE
}

enum class SkinScrollPaneStyle {
  DEFAULT, BRIGHT, NO_BGD
}

enum class SkinBarStyle {
  LIFE, MANA
}

fun configureSkin(assetStorage: AssetStorage): Skin {
  val atlas = assetStorage.loadSync(TextureAtlasAssets.UI.descriptor)

  KtxAsync.launch {
    if (!assetStorage.contains<Skin>("defaultSkin")) {
      assetStorage.add("defaultSkin", Scene2DSkin.defaultSkin)
    }
  }

  return Scene2DSkin.defaultSkin.also { skin ->
    skin.addRegions(atlas)

    // fonts
    SkinFontStyle.values().forEach { style ->
      skin[style.name] = BitmapFont(Gdx.files.internal(style.fntFilePath), atlas.findRegion(style.regionKey)).apply {
        data.markupEnabled = true
        data.setScale(style.scale)
      }
    }

    // labels
    skin.label(SkinLabelStyle.DEFAULT.name) {
      font = skin[SkinFontStyle.DEFAULT.name]
    }
    skin.label(SkinLabelStyle.LARGE.name) {
      font = skin[SkinFontStyle.LARGE.name]
    }
    skin.label(SkinLabelStyle.DUNGEON_LEVEL.name) {
      font = skin[SkinFontStyle.TITLE.name]
      background = skin.newDrawable(SkinImages.FRAME_3.regionKey).apply {
        leftWidth = 10f
        rightWidth = 10f
      }
    }
    skin.label(SkinLabelStyle.FRAMED_BRIGHT.name) {
      font = skin[SkinFontStyle.LARGE.name]
      background = skin.newDrawable(SkinImages.FRAME_2.regionKey).apply {
        leftWidth = 10f
        rightWidth = 10f
      }
    }

    // buttons
    skin.textButton(SkinTextButtonStyle.TITLE.name) {
      up = skin[SkinImages.FRAME_3.regionKey]
      font = skin[SkinFontStyle.TITLE.name]
    }
    skin.textButton(SkinTextButtonStyle.DEFAULT.name) {
      up = skin[SkinImages.BUTTON_1.regionKey]
      font = skin[SkinFontStyle.DEFAULT.name]
      disabledFontColor = Color.GRAY
    }
    skin.textButton(SkinTextButtonStyle.BRIGHT.name) {
      up = skin[SkinImages.BUTTON_2.regionKey]
      font = skin[SkinFontStyle.LARGE.name]
    }
    skin.textButton(SkinTextButtonStyle.GREEN.name) {
      up = skin.newDrawable(SkinImages.FRAME_GREEN.regionKey).apply {
        leftWidth = 5f
        rightWidth = 5f
        topHeight = 5f
        bottomHeight = 5f
      }
      font = skin[SkinFontStyle.TITLE.name]
    }
    skin.textButton(SkinTextButtonStyle.RED.name) {
      up = skin.newDrawable(SkinImages.FRAME_RED.regionKey).apply {
        leftWidth = 5f
        rightWidth = 5f
        topHeight = 5f
        bottomHeight = 5f
      }
      font = skin[SkinFontStyle.TITLE.name]
    }

    // list
    skin.list(SkinListStyle.DEFAULT.name) {
      font = skin[SkinFontStyle.DEFAULT.name]
      fontColorSelected = Color.BLACK
      fontColorUnselected = Color.WHITE
      selection = skin.newDrawable(SkinImages.BUTTON_2.regionKey).apply {
        topHeight = 1f
        bottomHeight = 1f
        leftWidth = 3f
      }
    }
    skin.list(SkinListStyle.LARGE.name) {
      font = skin[SkinFontStyle.LARGE.name]
      fontColorSelected = Color.BLACK
      fontColorUnselected = Color.WHITE
      selection = skin.newDrawable(SkinImages.BUTTON_2.regionKey).apply {
        topHeight = 1f
        bottomHeight = 1f
        leftWidth = 3f
      }
    }

    // scroll pane
    skin.scrollPane(SkinScrollPaneStyle.DEFAULT.name) {
      background = skin.newDrawable(SkinImages.FRAME_3.regionKey).apply {
        bottomHeight = 6f
      }
    }
    skin.scrollPane(SkinScrollPaneStyle.BRIGHT.name) {
      background = skin.newDrawable(SkinImages.BUTTON_2.regionKey).apply {
        topHeight = 1f
        bottomHeight = 1f
      }
    }
    skin.scrollPane(SkinScrollPaneStyle.NO_BGD.name)

    // bar
    skin.bar(SkinBarStyle.LIFE.name, skin[SkinImages.BAR_FRAME.regionKey], skin[SkinImages.BAR_RED.regionKey]) {
      barOffsetX = 1f
      barOffsetY = 2f
    }
    skin.bar(SkinBarStyle.MANA.name, skin[SkinImages.BAR_FRAME.regionKey], skin[SkinImages.BAR_BLUE.regionKey]) {
      barOffsetX = 1f
      barOffsetY = 2f
    }
  }
}
