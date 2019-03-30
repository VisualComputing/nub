uniform mat4 transform;
uniform mat4 modelview;
uniform mat3 normalMatrix;
uniform mat4 lightMatrix;
uniform mat4 mvINV;
//uniform mat4 shadowTransform;
//mat4 shadowTransform;
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

    // shadowCoord computation
    // 1. works like a shine
    vec4 vertPosition = modelview * vertex;
    mat4 shadowTransform = lightMatrix * mvINV;
    shadowCoord = shadowTransform * (vertPosition + vec4(vertNormal, 0.0));// Normal bias removes the shadow acne

    // 2. alternative is to compute it (the shadowCoord) without vertNormal,
    // but it creates shadow-artifact (e.g., no shadows on the floor):
    //shadowCoord = shadowTransform * vertPosition;

    // emulates previous shadow-artifact
    // shadowCoord = lightMatrix * mvINV * modelview * vertex;
    // TODO: really want to write the previous line as (but it doesn  work):
    // shadowCoord = lightMatrix * vertex;

    lightIntensity = 0.5 + dot(-lightDirection, vertNormal) * 0.5;
    gl_Position = transform * vertex;
}
