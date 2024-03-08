uniform mat2 transformationMatrix;
attribute vec2 vCoordinates;

void main() {
    vec2 transformedPosition = transformationMatrix * vCoordinates;
    gl_Position = vec4(transformedPosition, 0.0, 1.0);
}