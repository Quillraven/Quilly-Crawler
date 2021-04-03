package com.github.quillraven.commons.shader

import org.intellij.lang.annotations.Language

interface Shader {
  val id: String
  val vertexShader: String
  val fragmentShader: String

  companion object {
    val DEFAULT_SHADER = object : Shader {
      override val id = "commonsDefaultShader"
      override val vertexShader = DEFAULT_VERTEX
      override val fragmentShader = DEFAULT_FRAGMENT
    }

    val OUTLINE_SHADER = object : Shader {
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
uniform vec2 u_textureSize;
uniform vec4 u_regionBoundary;
uniform vec4 u_outlineColor;

void main()
{
  vec2 pixelSize = 1.0 / u_textureSize;
  if (texture2D(u_texture, v_texCoords).a == 0.0) {
    // transparent pixel -> check if it has adjacent non-transparent pixels
    // if yes it is a border pixel -> draw outline
    // if no it is surrounded by other transparent pixels -> do not draw outline
    if (texture2D(u_texture, v_texCoords + vec2(pixelSize.x, 0.0)).a > 0.0
    || texture2D(u_texture, v_texCoords + vec2(-pixelSize.x, 0.0)).a > 0.0
    || texture2D(u_texture, v_texCoords + vec2(0.0, pixelSize.y)).a > 0.0
    || texture2D(u_texture, v_texCoords + vec2(0.0, -pixelSize.y)).a > 0.0
    ) {
      // border pixel
      gl_FragColor = u_outlineColor;
      return;
    }
  } else {
    // check if pixel is an edge pixel and since it did not pass the transparent check before
    // we need to color it with the outline color to correctly highlight edge pixels
    if(v_texCoords.x <= (u_regionBoundary.x+pixelSize.x) || v_texCoords.x >= (u_regionBoundary.z-pixelSize.x)
        || v_texCoords.y <= (u_regionBoundary.y+pixelSize.y) || v_texCoords.y >= (u_regionBoundary.w-pixelSize.y)
    ) {
        // edge pixel
        gl_FragColor = u_outlineColor;
        return;
    }
  }

  gl_FragColor = vec4(u_outlineColor.rgb, 0.0);
}
"""
