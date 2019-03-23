uniform sampler2D shadowMap;
varying vec4 vertColor;
varying vec3 ecNormal;
varying vec3 lightDirection;
// shadow
varying vec4 shadowCoordinate;

void main() {
  vec3 direction = normalize(lightDirection);
  vec3 normal = normalize(ecNormal);
  float diffuse = max(0.0, dot(direction, normal));

  float shadow = 1.0;
  if (texture(shadowMap, shadowCoordinate.xy).z  < shadowCoordinate.z){
    shadow = 0.5;
  }

  gl_FragColor = vec4(shadow * diffuse, shadow * diffuse, shadow * diffuse, 1) * vertColor;
}