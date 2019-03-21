uniform sampler2D shadowMap;
varying vec4 vertColor;
varying vec3 ecNormal;
varying vec3 lightDirection;
// specular
varying vec3 cameraDirection;
varying vec3 lightDirectionReflected;
// shadow
varying vec4 FragPosLightSpace;

float ShadowCalculation(vec4 fragPosLightSpace) {
  // perform perspective divide
  vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
  // transform to [0,1] range
  projCoords = projCoords * 0.5 + 0.5;
  // get closest depth value from light's perspective (using [0,1] range fragPosLight as coords)
  float closestDepth = texture(shadowMap, projCoords.xy).r;
  // get depth of current fragment from light's perspective
  float currentDepth = projCoords.z;
  // check whether current frag pos is in shadow
  float shadow = currentDepth > closestDepth  ? 1.0 : 0.0;
  return shadow;
}

void main() {
  vec3 direction = normalize(lightDirection);
  vec3 normal = normalize(ecNormal);
  float diffuse = max(0.0, dot(direction, normal));

  vec3 lightDirectionReflectedNormalized = normalize(lightDirectionReflected);
  vec3 camera = normalize(cameraDirection);
  float specular = max(0.0, dot(lightDirectionReflectedNormalized, camera));

  float shadow = ShadowCalculation(FragPosLightSpace);
  float lighting = ((1.0 - shadow) * (diffuse + specular));
  //float lighting = diffuse + specular;
  //float lighting = ((1.0 - 1.0) * (diffuse + specular));
  gl_FragColor = vec4(lighting, lighting, lighting, 1) * vertColor;
}