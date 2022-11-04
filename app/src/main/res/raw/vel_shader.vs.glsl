attribute vec4 aPos;

uniform mat4 uMVP;

void main()
{
    gl_Position = uMVP * aPos;
}
