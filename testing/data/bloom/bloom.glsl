uniform sampler2D texture;
uniform sampler2D nuevoTex;
varying vec4 vertTexCoord;

void main(){
	vec4 colorOriginal = vec4(0.0, 0.0, 0.0, 0.0);
   	vec4 colorNuevo = vec4(0.0, 0.0, 0.0, 0.0);

	colorOriginal = texture2D(texture, vertTexCoord.st);
   	colorNuevo = texture2D(nuevoTex, vertTexCoord.st);

	gl_FragColor = vec4(colorOriginal + colorNuevo);
}
