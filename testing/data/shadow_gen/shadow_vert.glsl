uniform mat4 nub_transform;
uniform mat4 nub_modelview;
uniform mat4 shadowTransform;
uniform vec3 lightDirection;

attribute vec4 vertex;
attribute vec4 color;
attribute vec3 normal;

varying vec4 vertColor;
varying vec4 shadowCoord;
varying float lightIntensity;

void main() {
  vertColor = color;
  //vec3 ecPosition = vec3(modelview * position);//eye coordinate system
  //vec3 ecNormal = normalize(normalMatrix * normal);//eye coordinate system
  vec4 ecPosition = nub_modelview * vertex; // get vertex in eye space
  mat3 nub_normalmatrix = transpose(inverse(mat3(nub_modelview)));
  vec3 ecNormal = normalize(nub_normalmatrix * normal); // get normal in eye space
  shadowCoord = shadowTransform * (ecPosition + vec4(ecNormal, 0.0)); // Normal bias removes the shadow acne
  lightIntensity = 0.5 + dot(-lightDirection, ecNormal) * 0.5;
  gl_Position = nub_transform * vertex;
}
