attribute vec4 aPos;
attribute vec2 aTex;

varying vec2 vTex;

uniform mat4 uMVP;

void main()
{
    vTex = aTex;
    gl_Position = uMVP * aPos;
}
