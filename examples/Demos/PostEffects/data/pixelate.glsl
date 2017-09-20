uniform sampler2D tex;
uniform float xPixels;
uniform float yPixels;

varying vec4 vertTexCoord;

uniform sampler2D tDepth;
            
void main()
	{
        vec2 texCoords = vec2(floor(vertTexCoord.s * xPixels) / xPixels, floor(vertTexCoord.t * yPixels) / yPixels);
        gl_FragColor = texture2D(tex, texCoords);
        }
