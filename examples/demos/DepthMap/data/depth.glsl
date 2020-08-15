// The fragment eye depth is obtained by solving z_e in terms z_n from here:
// http://visualcomputing.github.io/Transformations/#/6/14 and:
// http://visualcomputing.github.io/Transformations/#/6/15
// yielding to: z_e = (2 * near * far) / (z_n * (far - near) - far - near) (eq1)

uniform float near;
uniform float far;

// remapping of a value among 2 ranges: http://visualcomputing.github.io/Transformations/#/7/1
// same as: https://processing.org/reference/map_.html
float map(float value, float start1, float stop1, float start2, float stop2) {
  return start2 + (value - start1) * (stop2 - start2) / (stop1 - start1);
}

void main() {
  // z_n is obtained by remapping gl_FragCoord.z from [0..1] to [-1..1]
  float z_n = map(gl_FragCoord.z, 0, 1, -1, 1);
  // eq 1
  float z_e = (2 * near * far) / (z_n * (far - near) - far - near);
  // the normalized eye depth is obtained by remapping z_e from [-near..-far] to [0..1]
  float depth = map(z_e, -near, -far, 0, 1);
  // render the depth as a grey scale color
  gl_FragColor = vec4(vec3(depth), 1);
}