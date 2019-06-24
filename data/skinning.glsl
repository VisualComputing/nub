uniform mat4 projection;
uniform mat4 modelview;
attribute vec4 color;
attribute vec4 position;
attribute vec4 joints;
attribute vec4 weights;
varying float dist[30];
uniform float[120] bonePositionOrig;
uniform float[120] bonePosition;
uniform float[120] boneRotation;
uniform int boneLength;
varying vec4 vertColor;

uniform mat4 texMatrix;
attribute vec2 texCoord;
varying vec4 vertTexCoord;
uniform int paintMode;

uniform mat3 normalMatrix;
uniform vec4 lightPosition;
attribute vec3 normal;

varying vec3 ecNormal;
varying vec3 lightDir;
uniform vec3 lightAmbient[8];

// All components are in the range [0…1], including hue.
// see  https://stackoverflow.com/questions/15095909/from-rgb-to-hsv-in-opengl-glsl
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 rot(vec4 quaternion, vec3 vector) {
//TODO : Update
    float q00 = 2.0f * quaternion[0] * quaternion[0];
    float q11 = 2.0f * quaternion[1] * quaternion[1];
    float q22 = 2.0f * quaternion[2] * quaternion[2];

    float q01 = 2.0f * quaternion[0] * quaternion[1];
    float q02 = 2.0f * quaternion[0] * quaternion[2];
    float q03 = 2.0f * quaternion[0] * quaternion[3];

    float q12 = 2.0f * quaternion[1] * quaternion[2];
    float q13 = 2.0f * quaternion[1] * quaternion[3];

    float q23 = 2.0f * quaternion[2] * quaternion[3];

    return vec3((1.0f - q11 - q22) * vector[0] + (q01 - q23) * vector[1] + (q02 + q13) * vector[2],
                   (q01 + q23) * vector[0] + (1.0f - q22 - q00) * vector[1] + (q12 - q03) * vector[2],
                   (q02 - q13) * vector[0] + (q12 + q03) * vector[1] + (1.0f - q11 - q00) * vector[2]);
}


void main() {
  vec4 curPos = position;
  vec3 curNor = normal;
  vec3 v = vec3(0.0);
  vec3 n = vec3(0.0);
  vec3 idd = vec3(1.0);

  for(int i = 0; i < 3; i++){
    int idx = int(joints[i]);
    if(weights[i] == 0) continue;
    vec4 quat = vec4(boneRotation[4*idx + 0], boneRotation[4*idx + 1], boneRotation[4*idx + 2], boneRotation[4*idx + 3]);
    vec3 pos = vec3(bonePositionOrig[3*idx + 0], bonePositionOrig[3*idx + 1], bonePositionOrig[3*idx + 2]);
    vec3 offset = vec3(bonePosition[3*idx + 0], bonePosition[3*idx + 1], bonePosition[3*idx + 2]);
    vec3 u = curPos.xyz;
    u = u - pos;
    u = rot(quat, u) + pos;
    u = u + offset;
    v = v + u*weights[i];
    n = n + rot(quat, normal)*weights[i];
  }

  vec4 ecPosition = modelview * vec4(v,1);
  gl_Position = projection * ecPosition;

  if(paintMode == -1){
      ecNormal = normalize(normalMatrix * n);
      lightDir = normalize(lightPosition.xyz - vec3(ecPosition));
      vertColor = color;
  } else if(paintMode == 0){
      ecNormal = vec3(1,1,1);
      lightDir = ecNormal;
      vertColor = vec4(hsv2rgb(vec3((joints[0]+1.0)/boneLength, 1.0 , 1.0)), 1);
  } else{
      if(joints[0] == paintMode){
          ecNormal = vec3(1,1,1);
          lightDir = ecNormal;
          vertColor = vec4(hsv2rgb(vec3((joints[0]+1.0)/boneLength, 1.0 , 1.0)), 1);
      }else{
          ecNormal = normalize(normalMatrix * n);
          lightDir = normalize(lightPosition.xyz - vec3(ecPosition));
          vertColor = color;
      }
  }
  vertTexCoord = texMatrix * vec4(texCoord, 1.0, 1.0);
}