#define KERNEL_SIZE 25

uniform float kernel[KERNEL_SIZE];
uniform sampler2D readTex;
uniform vec2 imageIncrement;

varying vec2 vUv;
varying vec4 vertTexCoord;
                                         
void main()
{
	vec2 imageCoord = vUv - ( ( ( float(KERNEL_SIZE) - 1.0 ) / 2.0 ) * imageIncrement );
	vec4 sum = vec4( 0.0, 0.0, 0.0, 0.0 );

	for( int i = 0; i < KERNEL_SIZE; i++ )
	{
        	sum += texture2D( readTex, imageCoord ) * kernel[ i ];
        	imageCoord += imageIncrement;
        }
               
        gl_FragColor = sum;
}
