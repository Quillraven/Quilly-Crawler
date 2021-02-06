package com.github.quillraven.quillycrawler

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.quillycrawler.input.InputServiceProvider
import com.github.quillraven.quillycrawler.screen.PlayGroundScreen

class QuillyCrawler : AbstractGame() {
    val inputServiceProvider by lazy { InputServiceProvider }

    override fun create() {
        if ("true" == System.getProperty("devMode", "false")) {
            Gdx.app.logLevel = Application.LOG_DEBUG
        }

        addScreen(PlayGroundScreen(this))
        setScreen<PlayGroundScreen>()
    }

    companion object {
        const val UNIT_SCALE = 1 / 16f
    }
}