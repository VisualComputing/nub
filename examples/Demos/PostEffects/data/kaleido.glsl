uniform sampler2D tex;
uniform float segments;
             
varying vec4 vertTexCoord;

void main()
	{
	vec2 uv = vertTexCoord.st;
	vec2 normed = 2.0 * uv - 1.0;
        float r = length(normed);
        float theta = atan(normed.y / abs(normed.x));
        theta *= segments;
                
        vec2 newUv = (vec2(r * cos(theta), r * sin(theta)) + 1.0) / 2.0;
                
        gl_FragColor = texture2D(tex, newUv);
}
