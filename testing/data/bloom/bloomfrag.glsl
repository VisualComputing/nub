#define KERNEL_SIZE 25

uniform float kernel[KERNEL_SIZE];
uniform sampler2D readTex;
uniform vec2 imageIncrement;
uniform vec2 resolution;

//varying vec2 vUv;
//varying vec2 scaledImageIncrement;
varying vec4 vertTexCoord;
                                         
void main()
{
	vec2 scaledImageIncrement = imageIncrement * resolution;
	vec2 imageCoord = vertTexCoord.st - ( ( float(KERNEL_SIZE) - 1.0 ) / 2.0 ) * scaledImageIncrement;
	vec4 sum = vec4( 0.0, 0.0, 0.0, 0.0 );

	for( int i = 0; i < KERNEL_SIZE; i++ )
	{
        	sum += texture2D( readTex, imageCoord ) * kernel[ i ];
        	imageCoord += scaledImageIncrement;
        }
               
        gl_FragColor = sum;
}
