attribute vec3 pos;
attribute vec2 uv;
attribute vec4 color;

varying lowp vec4 vColor;
varying highp vec2 vUv;

uniform highp vec2 screen;
uniform mat4 mvp;

void main() {
	vColor = color;
	vUv = uv;
	
    gl_Position = mvp * vec4(pos.xyz, 1.0);
}
