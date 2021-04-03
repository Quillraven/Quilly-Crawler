package com.github.quillraven.commons.shader

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.github.quillraven.commons.map.MapService

interface PostProcessRenderer {
  fun postProcess(batch: Batch, entities: ImmutableArray<Entity>, mapService: MapService) = Unit
}

interface ShaderService {
  fun shader(shader: Shader): ShaderProgram
}
