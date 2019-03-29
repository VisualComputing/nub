uniform mat4 light_transform;

attribute vec4 vertex;

void main() {
  gl_Position = light_transform * vertex;
}
