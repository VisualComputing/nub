uniform mat4 nub_transform;

attribute vec4 vertex;
attribute vec4 color;

varying vec4 vertColor;

void main() {
  gl_Position = nub_transform * vertex;
  vertColor = color;
}
