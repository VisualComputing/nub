uniform mat4 transform;
uniform mat4 modelview;
uniform mat3 normalMatrix;
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
    vec3 vertNormal = normalize(normalMatrix * normal);// Get normal direction in model view space
    vec4 vertPosition = modelview * vertex;
    shadowCoord = shadowTransform * (vertPosition + vec4(vertNormal, 0.0));// Normal bias removes the shadow acne
    lightIntensity = 0.5 + dot(-lightDirection, vertNormal) * 0.5;
    gl_Position = transform * vertex;
}
