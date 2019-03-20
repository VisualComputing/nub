#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D shadowMap;
varying vec4 vertColor;
varying vec3 ecNormal;
varying vec3 lightDirection;
// specular
varying vec3 cameraDirection;
varying vec3 lightDirectionReflected;

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
  /*
  vec3 direction = normalize(lightDirection);
  vec3 normal = normalize(ecNormal);
  float intensity = max(0.0, dot(direction, normal));
  gl_FragColor = vec4(intensity, intensity, intensity, 1) * vertColor;
  */

  /*
  vec3 color = texture(diffuseTexture, fs_in.TexCoords).rgb;
  vec3 normal = normalize(fs_in.Normal);
  vec3 lightColor = vec3(1.0);
  // ambient
  vec3 ambient = 0.15 * color;
  // diffuse
  vec3 lightDirection = normalize(lightPos - fs_in.FragPos);
  float diff = max(dot(lightDirection, normal), 0.0);
  vec3 diffuse = diff * lightColor;
  // specular
  vec3 viewDir = normalize(viewPos - fs_in.FragPos);
  float spec = 0.0;
  vec3 halfwayDir = normalize(lightDirection + viewDir);
  spec = pow(max(dot(normal, halfwayDir), 0.0), 64.0);
  vec3 specular = spec * lightColor;
  // calculate shadow
  float shadow = ShadowCalculation(fs_in.FragPosLightSpace);
  vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * color;

  FragColor = vec4(lighting, 1.0);
  */

  vec3 direction = normalize(lightDirection);
  vec3 normal = normalize(ecNormal);
  float diffuse = max(0.0, dot(direction, normal));
  //gl_FragColor = vec4(diffuse, diffuse, diffuse, 1) * vertColor;

  vec3 lightDirectionReflectedNormalized = normalize(lightDirectionReflected);
  vec3 camera = normalize(cameraDirection);
  float specular = max(0.0, dot(lightDirectionReflectedNormalized, camera));
  //gl_FragColor = vec4(specular, specular, specular, 1) * vertColor;

  // calculate shadow
  //float shadow = ShadowCalculation(fs_in.FragPosLightSpace);
  //vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * vertColor;
  //gl_FragColor = vec4(lighting, 1.0);

  //float shadow = ShadowCalculation(fs_in.FragPosLightSpace);
  //vec3 lighting = ((1.0 - shadow) * (diffuse + specular)) * vertColor;
  //vec3 lighting = ((1.0 - shadow) * (diffuse + specular)) * vertColor;
  //gl_FragColor = vec4(lighting, 1.0);

  //float shadow = ShadowCalculation(fs_in.FragPosLightSpace);
  //float lighting = ((1.0 - shadow) * (diffuse + specular));
  float lighting = diffuse + specular;
  gl_FragColor = vec4(lighting, lighting, lighting, 1) * vertColor;
}