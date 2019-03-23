uniform mat4 modelview;
uniform mat4 transform;
uniform mat3 normalMatrix;
uniform vec4 lightPosition;
//custom
uniform mat4 lightSpaceMatrix;

attribute vec4 position;
attribute vec4 color;
attribute vec3 normal;

// lights
varying vec3 ecNormal;
varying vec3 lightDirection;
// shadow
varying vec4 shadowCoordinate;
// color
varying vec4 vertColor;

void main() {
  gl_Position = transform * position;
  // shadow
  shadowCoordinate = lightSpaceMatrix * position;
  vec3 ecPosition = vec3(modelview * position);
  ecNormal = normalize(normalMatrix * normal);
  lightDirection = normalize(lightPosition.xyz - ecPosition);
  // color
  vertColor = color;
}