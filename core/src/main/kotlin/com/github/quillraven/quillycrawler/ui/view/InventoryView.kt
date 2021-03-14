package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import ktx.scene2d.scene2d
import ktx.scene2d.table
import ktx.scene2d.textButton

fun inventoryView(skin: Skin) = scene2d.table {
  setFillParent(true)
  background = skin.getDrawable(SkinImages.WINDOW.regionKey)

  textButton("Inventory", SkinTextButtonStyle.TITLE.name) { cell ->
    this.labelCell.padTop(4f)
    cell.expand()
      .top().padTop(8f)
      .height(25f).width(95f)
      .colspan(2)
      .row()
  }

  table { cell ->
    background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

    cell.expand()
      .padBottom(3f)
      .width(95f).height(115f)
  }

  table { cell ->
    background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

    cell.expand()
      .padBottom(3f)
      .width(180f).height(115f)
      .row()
  }

  textButton("Press >", SkinTextButtonStyle.DEFAULT.name) { cell ->
    cell.padBottom(7f)
  }
}
