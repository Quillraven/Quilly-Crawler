package com.github.quillraven.quillycrawler.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.assets.async.AssetStorage
import ktx.scene2d.Scene2DSkin
import ktx.style.get
import ktx.style.set
import ktx.style.textButton

enum class SkinImages(val regionKey: String) {
  BUTTON_1("button-1"),
  WINDOW("window-1"),
  FRAME_1("frame-1"),
  FRAME_3("frame-3"),
}

enum class SkinFontStyle(val fntFilePath: String, val regionKey: String, val scale: Float) {
  TITLE("fonts/dungeonFont.fnt", "dungeonFont", 0.5f),
  DEFAULT("fonts/dungeonFont.fnt", "dungeonFont", 0.25f),
}

enum class SkinTextButtonStyle {
  TITLE, DEFAULT
}

fun configureSkin(assetStorage: AssetStorage): Skin {
  val atlas = assetStorage.loadSync(TextureAtlasAssets.UI.descriptor)

  return Scene2DSkin.defaultSkin.also { skin ->
    skin.addRegions(atlas)

    // fonts
    SkinFontStyle.values().forEach { style ->
      skin[style.name] = BitmapFont(Gdx.files.internal(style.fntFilePath), atlas.findRegion(style.regionKey)).apply {
        data.markupEnabled = true
        data.setScale(style.scale)
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
    }
  }
}
