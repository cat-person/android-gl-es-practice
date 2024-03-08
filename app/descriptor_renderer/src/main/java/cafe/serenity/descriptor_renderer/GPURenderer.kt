package cafe.serenity.descriptor_renderer

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val EGL_CONTEXT_CLIENT_VERSION =
    2 // Don't forget surfaceView.setEGLContextClientVersion(EGL_CONTEX_CLIENT_VERSION)

interface GPUProgram {
    val vShaderCode: String
    val fShaderCode: String

    val vCoordinates: FloatArray
    val scaleX: Float
    val scaleY: Float
    val rotationDegrees: Float
    val color: FloatArray
}

object GPURenderer {
    // const
    private val coordsPerVertex = 2
    private val coordsByteSize = 4

    // handles
    lateinit var gpuProgram: GPUProgram
    private var vShaderHandle: Int = 0
    private var fShaderHandle: Int = 0

    private var programHandle: Int = 0

    private val vCoordinatesAttributeLocation by lazy {
        GLES20.glGetAttribLocation(programHandle, "vCoordinates");
    }

    private val transformationMatrixLocation by lazy {
        GLES20.glGetUniformLocation(programHandle, "transformationMatrix");
    }
    private val colorUniformLocation by lazy {
        GLES20.glGetUniformLocation(programHandle, "color");
    }

    // params
    private var transformationMatrix = floatArrayOf(
        1f, 0f,
        0f, 1f,
    )

    private var color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)

    fun createProgram(gpuProgram: GPUProgram) {
        GPURenderer.gpuProgram = gpuProgram

        vShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, gpuProgram.vShaderCode)
        fShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, gpuProgram.fShaderCode)

        programHandle = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vShaderHandle)
            GLES20.glAttachShader(it, fShaderHandle)
            GLES20.glLinkProgram(it)
        }

        updateParams(
            gpuProgram.scaleX,
            gpuProgram.scaleY,
            gpuProgram.rotationDegrees,
            gpuProgram.color
        )
    }

    fun updateDescriptor(gpuProgram: GPUProgram) {
        updateParams(
            gpuProgram.scaleX,
            gpuProgram.scaleY,
            gpuProgram.rotationDegrees,
            gpuProgram.color
        )
    }

    private fun updateParams(
        scaleX: Float,
        scaleY: Float,
        rotationDegrees: Float,
        color: FloatArray
    ) {
        val radians = (PI.toFloat() * rotationDegrees) / 180f

        transformationMatrix = floatArrayOf(
            scaleX * cos(radians), scaleY * -sin(radians),
            scaleX * sin(radians), scaleY * cos(radians),
        )
        GPURenderer.color = color
    }

    fun render() {
        GLES20.glUseProgram(programHandle)

        // get handle to vertex shader's vPosition member
        GLES20.glEnableVertexAttribArray(vCoordinatesAttributeLocation)

        val requiredBufferSize = coordsByteSize * gpuProgram.vCoordinates.size

        val buffer = ByteBuffer.allocateDirect(requiredBufferSize).also {
            it.order(ByteOrder.nativeOrder())
            it.asFloatBuffer().put(gpuProgram.vCoordinates)
        }

        GLES20.glVertexAttribPointer(
            vCoordinatesAttributeLocation,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex * coordsByteSize,
            buffer
        )

        GLES20.glUniform4fv(colorUniformLocation, 1, color, 0)

        GLES20.glUniformMatrix2fv(transformationMatrixLocation, 1, false, transformationMatrix, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(vCoordinatesAttributeLocation)
    }

    fun terminate() {
        GLES20.glDeleteBuffers(1, IntBuffer.wrap(intArrayOf(vCoordinatesAttributeLocation)))
        GLES20.glDeleteProgram(programHandle);
    }
}

// compileShader
private fun loadShader(type: Int, shaderCode: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
    }
}