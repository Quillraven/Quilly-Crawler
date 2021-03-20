package com.github.quillraven.quillycrawler.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.github.quillraven.quillycrawler.QuillyCrawler

fun main() {
  Lwjgl3Application(QuillyCrawler(), Lwjgl3ApplicationConfiguration().apply {
    setTitle("Quilly Crawler")
    setWindowedMode(1200, 675)
    setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
  })
}
