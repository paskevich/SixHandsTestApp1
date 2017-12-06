attribute vec4 a_Position;
uniform mat4 u_ProjMatrix;
//uniform mat4 u_ModelMatrix;

void main() {
    //gl_Position = u_ProjMatrix * a_Position;
    gl_Position = a_Position;
}
