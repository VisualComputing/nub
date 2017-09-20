#define KERNEL_SIZE 25.0

uniform vec2 imageIncrement;
uniform vec2 resolution;
uniform mat4 modelview;
uniform mat4 projection;
uniform mat4 texMatrix;

attribute vec4 vertex;
attribute vec2 texCoord;

varying vec2 vUv;
varying vec2 scaledImageIncrement;
varying vec4 vertTexCoord;
          
void main()
{
	vertTexCoord = texMatrix * vec4(texCoord, 1.0, 1.0);
	scaledImageIncrement = imageIncrement * resolution;
	vUv = vertTexCoord.st - ( ( KERNEL_SIZE - 1.0 ) / 2.0 ) * scaledImageIncrement;
	gl_Position = projection * modelview * vertex;
}
