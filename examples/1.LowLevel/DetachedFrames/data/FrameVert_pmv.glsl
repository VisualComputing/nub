uniform mat4 frames_transform;

attribute vec4 vertex;
attribute vec4 color;

varying vec4 vertColor;

void main() {
  gl_Position = frames_transform * vertex;
  vertColor = color;
}
