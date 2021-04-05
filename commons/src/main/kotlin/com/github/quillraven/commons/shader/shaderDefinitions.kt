package com.github.quillraven.commons.shader

import org.intellij.lang.annotations.Language

interface ShaderDefinition {
  val id: String
  val vertexShader: String
  val fragmentShader: String

  companion object {
    val DEFAULT_SHADER = object : ShaderDefinition {
      override val id = "commonsDefaultShader"
      override val vertexShader = DEFAULT_VERTEX
      override val fragmentShader = DEFAULT_FRAGMENT
    }

    val OUTLINE_SHADER = object : ShaderDefinition {
      override val id = "commonsOutlineShader"
      override val vertexShader = DEFAULT_VERTEX
      override val fragmentShader = OUTLINE_FRAGMENT
    }
  }
}

@Language("GLSL")
const val DEFAULT_VERTEX = """
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

varying vec4 v_color;
varying vec2 v_texCoords;

uniform mat4 u_projTrans;

void main()
{
  v_color = a_color;
  v_color.a = v_color.a * (255.0 / 254.0);
  v_texCoords = a_texCoord0;
  gl_Position =  u_projTrans * a_position;
}
"""

@Language("GLSL")
const val DEFAULT_FRAGMENT = """
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;

void main()
{
  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
}
"""

@Language("GLSL")
const val OUTLINE_FRAGMENT = """
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;

void main()
{
  vec2 pixelSize = 1.0 / textureSize(u_texture, 0).xy;

  // get alpha of surrounding pixels
  float surroundingA = 0.0;
  for(int x = -1; x<=1; x++) {
    for(int y = -1; y<=1; y++) {
      if(x==0 && y==0) {
        continue;
      }

      surroundingA += texture2D(u_texture, v_texCoords + vec2(x,y) * pixelSize).a;
    }
  }

  // if one of the surrounding pixels is transparent then this pixel will be an outline pixel
  vec4 pixel = texture2D(u_texture, v_texCoords);
  if(8.0 * pixel.a - surroundingA > 0.0) {
    gl_FragColor = v_color;
  } else {
   gl_FragColor = vec4(0.0);
  }
}
"""
