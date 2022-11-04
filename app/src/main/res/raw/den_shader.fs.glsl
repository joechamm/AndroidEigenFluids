precision mediump float;

varying vec2 vTex;

uniform sampler2D uDensity;
uniform vec4 uColor;

void main()
{
    float d = texture2D(uDensity, vTex).r;
    vec3 color = uColor.rgb * d;
    gl_FragColor = vec4(color, 1.0);
}
