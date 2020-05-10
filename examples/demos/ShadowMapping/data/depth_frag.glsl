vec4 packDepth(float depth) {
    float depthFrac = fract(depth * 255.0);
    return vec4(depth - depthFrac / 255.0, depthFrac, 1.0, 1.0);
}

void main(void) {
    gl_FragColor = packDepth(gl_FragCoord.z);
}