package com.github.quillraven.quillycrawler.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.I18NBundle

enum class TextureAtlasAssets(
  filePath: String,
  val descriptor: AssetDescriptor<TextureAtlas> = AssetDescriptor(filePath, TextureAtlas::class.java)
) {
  ENTITIES("graphics/entities.atlas"),
  UI("graphics/ui.atlas")
}

enum class I18NAssets(
  filePath: String,
  val descriptor: AssetDescriptor<I18NBundle> = AssetDescriptor(filePath, I18NBundle::class.java)
) {
  DEFAULT("i18n")
}

enum class MusicAssets(
  filePath: String,
  val descriptor: AssetDescriptor<Music> = AssetDescriptor(filePath, Music::class.java)
) {
  TAKE_COVER("audio/music/Take Cover.ogg"),
  TRY_AND_SOLVE_THIS("audio/music/Try and Solve This.ogg")
}

enum class SoundAssets(
  filePath: String,
  val descriptor: AssetDescriptor<Sound> = AssetDescriptor(filePath, Sound::class.java)
) {
  CHEST_OPEN("audio/sounds/chest_open.mp3"),
  DROP("audio/sounds/drop.mp3"),
  MENU_SELECT("audio/sounds/menu_select.mp3"),
  MENU_SELECT_2("audio/sounds/menu_select_2.mp3"),
}
