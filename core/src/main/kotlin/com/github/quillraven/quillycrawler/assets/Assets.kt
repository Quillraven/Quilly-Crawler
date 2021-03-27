package com.github.quillraven.quillycrawler.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.audio.AudioService

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
  POWER_UP_12("audio/sounds/power_up_12.wav"),
  AMBIENCE_CAVE("audio/sounds/ambience_cave.mp3"),
  DRAGON_GROWL_00("audio/sounds/Dragon_Growl_00.mp3"),
  DRAGON_GROWL_01("audio/sounds/Dragon_Growl_01.mp3"),
  GOBLIN_03("audio/sounds/Goblin_03.mp3"),
}

fun AudioService.play(asset: SoundAssets, volume: Float = 1f, loop: Boolean = false) =
  this.playSound(asset.descriptor.fileName, volume, loop)

fun AudioService.stop(asset: SoundAssets) =
  this.stopSounds(asset.descriptor.fileName)
