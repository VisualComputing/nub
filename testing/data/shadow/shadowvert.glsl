uniform mat4 modelview;
uniform mat4 transform;
uniform mat3 normalMatrix;
uniform vec4 lightPosition;
//uniform vec3 lightNormal;

attribute vec4 position;
attribute vec4 color;
attribute vec3 normal;

// lights
varying vec3 ecNormal;
varying vec3 lightDirection;
// specular
varying vec3 cameraDirection;
varying vec3 lightDirectionReflected;

// color
varying vec4 vertColor;

void main() {
  gl_Position = transform * position;    
  vec3 ecPosition = vec3(modelview * position);
  ecNormal = normalize(normalMatrix * normal);
  lightDirection = normalize(lightPosition.xyz - ecPosition);
  // specular
  cameraDirection = normalize(0 - ecPosition);
  lightDirectionReflected = reflect(-lightDirection, ecNormal);
  // color
  vertColor = color;
}