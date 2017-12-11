precision mediump float;

uniform sampler2D u_ImageTex;
uniform sampler2D u_MaskTex;

varying vec2 v_Texture;

void main() {
    if(texture2D(u_MaskTex, v_Texture).g == 1.0) {
        gl_FragColor = vec4(0, 0, 0, 1);
    } else {
        gl_FragColor = texture2D(u_ImageTex, v_Texture);
    }
}