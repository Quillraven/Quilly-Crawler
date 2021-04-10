package com.github.quillraven.commons.shader

import org.intellij.lang.annotations.Language

interface ShaderDefinition {
  val id: String
  val vertexShader: String
  val fragmentShader: String

  companion object {
    val OUTLINE_SHADER = object : ShaderDefinition {
      override val id = "commonsOutlineShader"
      override val vertexShader = DEFAULT_VERTEX
      override val fragmentShader = OUTLINE_FRAGMENT
    }

    val BLUR_SHADER = object : ShaderDefinition {
      override val id = "commonsBlurShader"
      override val vertexShader = DEFAULT_VERTEX
      override val fragmentShader = BLUR_FRAGMENT
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

@Language("GLSL")
const val BLUR_FRAGMENT = """
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_direction;
uniform float u_radius;

void main() {
  vec4 sum = vec4(0.0);

  // Number of pixels off the central pixel to sample from
  float blur = u_radius / textureSize(u_texture, 0).xy;

  // Apply blur using 9 samples and predefined gaussian weights
  sum += texture2D(u_texture, v_texCoords - 4.0 * blur * u_direction) * 0.006;
  sum += texture2D(u_texture, v_texCoords - 3.0 * blur * u_direction) * 0.044;
  sum += texture2D(u_texture, v_texCoords - 2.0 * blur * u_direction) * 0.121;
  sum += texture2D(u_texture, v_texCoords - 1.0 * blur * u_direction) * 0.194;

  sum += texture2D(u_texture, v_texCoords) * 0.27;

  sum += texture2D(u_texture, v_texCoords + 1.0 * blur * u_direction) * 0.194;
  sum += texture2D(u_texture, v_texCoords + 2.0 * blur * u_direction) * 0.121;
  sum += texture2D(u_texture, v_texCoords + 3.0 * blur * u_direction) * 0.044;
  sum += texture2D(u_texture, v_texCoords + 4.0 * blur * u_direction) * 0.006;

  gl_FragColor = sum;
}
"""
