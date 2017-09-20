uniform float maxDepth;

void main() {
  float depth = gl_FragCoord.z / gl_FragCoord.w;
  gl_FragColor = vec4(vec3(1.0 - depth/maxDepth), 1.0);
}