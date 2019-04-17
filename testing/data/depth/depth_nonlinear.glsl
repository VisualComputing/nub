// took from: https://learnopengl.com/Advanced-OpenGL/Depth-testing

void main() {
  // gl_FragCoord.z is in the range [0..1]
  gl_FragColor = vec4(vec3(gl_FragCoord.z), 1.0);
}