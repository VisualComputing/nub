uniform mat4 nub_transform;

attribute vec4 vertex;

void main() {
  gl_Position = nub_transform * vertex;
}
