float distanceToLine(vec3 l0, vec3 l1, vec3 p){
    //is the distance between line formed by b and its parent and v
    vec3 line = l1 - l0;
    vec3 projection = p - l0;
    float dot = dot(projection, line);
    float magnitude = length(line);
    float u  = dot*1.0/(magnitude*magnitude);
    vec3 distance = vec3(0);
    if(u >= 0 && u <=1){
        distance = l0 + u*line;
        distance= distance - p;
    }
    else{
        distance= p - l0;
    }
    return length(distance);
}

vec3 rot(vec4 quaternion, vec3 vector) {
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
                   (q02 - q13) * vector[0] + (q12 + q03) * vector[1] + (1.0f - q11 - q00) * vector[2]);;
}

uniform mat4 transform;
attribute vec4 color;
attribute vec4 position;
uniform float[120] bonePosition;
uniform float[120] boneRotation;
uniform int boneLength;
varying vec4 vertColor;

void main() {
  vec4 curPos = transform * position;
  vec3 v = vec3(0.0);
  float totalDist = 0;
  //Get distance to each vertex and store weight
  //Get distance total distance and normalize
  //apply rotation and average position
  for(int i = 0, j = 0; i < boneLength -3; i+=3, j+=4){
    //Apply quat rotation
    vec4 quat = vec4(boneRotation[j + 0], boneRotation[j + 1], boneRotation[j + 2], boneRotation[j + 3]);
    vec3 parent = vec3(bonePosition[i + 0], bonePosition[i + 1], bonePosition[i + 2]);
    vec3 child = vec3(bonePosition[i + 3], bonePosition[i + 4], bonePosition[i + 5]);

    vec3 pos = curPos.xyz - parent;
    pos = parent + rot(quat, pos);
    float dist = 1.0/length(pos);//1.0/distanceToLine(parent, child, curPos.xyz);
    v = v + pos * dist;

    totalDist = totalDist + dist;
  }

  v = 1.0/totalDist * v;
  //v = normalize(v) * length(curPos);
  //gl_Position = curPos; //vec4(v,0);
  gl_Position = vec4(v,1);
  vertColor = color;
}
