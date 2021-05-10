package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Engine
import com.github.quillraven.commons.audio.AudioService

data class CombatContext(val engine: Engine, val audioService: AudioService)
