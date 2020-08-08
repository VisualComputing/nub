// took verbatim from here:
// https://github.com/processing/processing/blob/master/core/src/processing/opengl/shaders/PointVert.glsl

uniform mat4 projectionMatrix;
uniform mat4 modelviewMatrix;

uniform vec4 viewport;
uniform int perspective;

attribute vec4 position;
attribute vec4 color;
attribute vec2 offset;

void main() {
  vec4 pos = modelviewMatrix * position;
  vec4 clip = projectionMatrix * pos;

  // Perspective ---
  // convert from world to clip by multiplying with projection scaling factor
  // invert Y, projections in Processing invert Y
  vec2 perspScale = (projectionMatrix * vec4(1, -1, 0, 0)).xy;

  // formula to convert from clip space (range -1..1) to screen space (range 0..[width or height])
  // screen_p = (p.xy/p.w + <1,1>) * 0.5 * viewport.zw

  // No Perspective ---
  // multiply by W (to cancel out division by W later in the pipeline) and
  // convert from screen to clip (derived from clip to screen above)
  vec2 noPerspScale = clip.w / (0.5 * viewport.zw);

  gl_Position.xy = clip.xy + offset.xy * mix(noPerspScale, perspScale, float(perspective > 0));
  gl_Position.zw = clip.zw;
}