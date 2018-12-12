// took from: https://learnopengl.com/Advanced-OpenGL/Depth-testing

uniform float near;
uniform float far;

float linearizeDepth(float depth) {
  float z = depth * 2.0 - 1.0; // back to NDC
  return (2.0 * near * far) / (far + near - z * (far - near));
}

void main() {
  float depth = linearizeDepth(gl_FragCoord.z) / far; // divide by far for demonstration
  gl_FragColor = vec4(vec3(depth), 1.0);
}