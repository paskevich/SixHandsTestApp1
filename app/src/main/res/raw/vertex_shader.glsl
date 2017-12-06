attribute vec4 a_Position;
uniform mat4 u_ModelMatrix;
uniform mat4 u_ProjMatrix;
//uniform mat4 u_ViewMatrix;
attribute vec2 a_Texture;
varying vec2 v_Texture;

void main() {
    /*mat4 mvp = u_ProjMatrix * u_ModelMatrix;
    gl_Position = mvp * a_Position;*/
    gl_Position = u_ProjMatrix * u_ModelMatrix * a_Position;
    v_Texture = a_Texture;
}
