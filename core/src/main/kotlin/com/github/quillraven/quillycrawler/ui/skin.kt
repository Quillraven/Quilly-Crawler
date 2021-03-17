package com.github.quillraven.quillycrawler.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.assets.async.AssetStorage
import ktx.scene2d.Scene2DSkin
import ktx.style.*

enum class SkinImages(val regionKey: String) {
  UNDEFINED("undefined"),
  BUTTON_1("button-1"),
  BUTTON_2("button-2"),
  WINDOW("window-1"),
  FRAME_1("frame-1"),
  FRAME_3("frame-3"),
  GAME_PAD_DOWN("gamepad-down"),
  GAME_PAD_UP("gamepad-up"),
  GAME_PAD_A("gamepad-a"),
  GAME_PAD_B("gamepad-b"),
  KEY_BOARD_SPACE("button-space"),
  KEY_BOARD_UP("button-up"),
  KEY_BOARD_DOWN("button-down"),
  KEY_BOARD_ESCAPE("button-esc"),
}

enum class SkinFontStyle(val fntFilePath: String, val regionKey: String, val scale: Float) {
  TITLE("fonts/dungeonFont.fnt", "dungeonFont", 0.5f),
  DEFAULT("fonts/dungeonFont.fnt", "dungeonFont", 0.25f),
}

enum class SkinTextButtonStyle {
  TITLE, DEFAULT,
}

enum class SkinLabelStyle {
  DEFAULT
}

enum class SkinListStyle {
  DEFAULT
}

fun configureSkin(assetStorage: AssetStorage): Skin {
  val atlas = assetStorage.loadSync(TextureAtlasAssets.UI.descriptor)
  assetStorage.loadSync(I18NAssets.DEFAULT.descriptor)

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

    // buttons
    skin.textButton(SkinTextButtonStyle.TITLE.name) {
      up = skin[SkinImages.FRAME_3.regionKey]
      font = skin[SkinFontStyle.TITLE.name]
    }
    skin.textButton(SkinTextButtonStyle.DEFAULT.name) {
      up = skin[SkinImages.BUTTON_1.regionKey]
      font = skin[SkinFontStyle.DEFAULT.name]
    }

    // list
    skin.list(SkinListStyle.DEFAULT.name) {
      font = skin[SkinFontStyle.DEFAULT.name]
      fontColorSelected = Color.BLACK
      fontColorUnselected = Color.WHITE
      selection = skin.getDrawable(SkinImages.BUTTON_2.regionKey).apply {
        topHeight = 4f
        leftWidth = 3f
      }
    }
  }
}
