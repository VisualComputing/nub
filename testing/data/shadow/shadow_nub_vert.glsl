uniform mat4 nub_projection;
uniform mat4 nub_view;
uniform mat4 nub_model;
uniform mat4 lightMatrix;
uniform vec3 lightDirection;

attribute vec4 vertex;
attribute vec4 color;
attribute vec3 normal;

varying vec4 vertColor;
varying vec4 shadowCoord;
varying float lightIntensity;

void main() {
    vertColor = color;
    // transpose(inverse(mat3(nub_model))) leads the normalMatrix
    vec3 vertNormal = normalize(transpose(inverse(mat3(nub_model))) * normal);// Get normal direction in model view space
    vec4 vertPosition = nub_model * vertex;
    shadowCoord = lightMatrix * (vertPosition + vec4(vertNormal, 0.0));// Normal bias removes the shadow acne
    lightIntensity = 0.5 + dot(-lightDirection, vertNormal) * 0.5;
    gl_Position = nub_projection * nub_view * nub_model * vertex;
}
