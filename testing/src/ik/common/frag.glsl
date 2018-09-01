#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;
uniform int boneLength;

uniform vec2 texOffset;

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main() {
  //TODO : Update
  gl_FragColor = texture2D(texture, vertTexCoord.st) * vertColor;
  //gl_FragColor = vertColor;
}