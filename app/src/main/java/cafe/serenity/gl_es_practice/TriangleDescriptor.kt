package cafe.serenity.gl_es_practice

class TriangleDescriptor(
    override val vShaderCode: String,
    override val fShaderCode: String,
    override val vCoordinates: FloatArray): GPUProgram {

    override val scaleX: Float
        get() = _scaleX
    private var _scaleX: Float = 1f

    override val scaleY: Float
        get() = _scaleY
    private var _scaleY: Float = 1f
    fun setScale(scaleX: Float, scaleY: Float){
        _scaleX = scaleX
        _scaleY = scaleY
    }

    override val rotationDegrees: Float
        get() = _rotationDegrees
    private var _rotationDegrees: Float = 0f

    fun setRotation(rotationDegrees: Float) {
        _rotationDegrees = rotationDegrees
    }
    override val color: FloatArray
        get() = _color
    private var _color: FloatArray = floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f)

    fun setColor(color: FloatArray) {
        _color = color
    }
    companion object {
        const val vShaderAsset = "triangle_vertex_shader.glsl"
        const val fShaderAsset = "triangle_fragment_shader.glsl"
    }
}