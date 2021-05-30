package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Engine
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.event.GameEventDispatcher

data class CombatContext(val engine: Engine, val audioService: AudioService, val eventDispatcher: GameEventDispatcher)
