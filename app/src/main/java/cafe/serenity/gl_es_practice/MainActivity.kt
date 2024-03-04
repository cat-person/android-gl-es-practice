package cafe.serenity.gl_es_practice

import android.graphics.Color
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.graphics.toColor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val glSurfaceView = GLSurfaceView(this).also {
            it.setEGLContextClientVersion(2)
            it.setRenderer(object: Renderer{
                private lateinit var triangle: GLShape

                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                    assert(gl != null)
                    gl?.apply {
                        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
                    }
                    triangle = GLShape(MonochromaticTriangleGLDescriptor(arrayOf(PointF(-.5f, -.5f), PointF(.0f, .8f), PointF(.5f, -.5f)), Color.BLACK.toColor()))
                }

                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                    GLES20.glViewport(0, 0, width, height)
                    gl?.apply {
                        glViewport(0, 0, width, height)

                        // make adjustments for screen ratio
                        val ratio: Float = width.toFloat() / height.toFloat()

                        glMatrixMode(GL10.GL_PROJECTION)                                   // set matrix to projection mode
                        glLoadIdentity()                                                   // reset the matrix to its default state
                        glFrustumf(-ratio, ratio, -1f, 1f, 3f, 7f)  // apply the projection matrix
                    }
                }

                override fun onDrawFrame(gl: GL10?) {
                    gl?.apply {
                        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    }
                    triangle.draw()
                }
            })
        }

        setContentView(glSurfaceView)
    }
}


interface Shape2DGLDescriptor {
    val vShaderCode: String
    val fShaderCode: String
    val transformationMatrix: FloatArray // 2x2 matrix
}


sealed class Attribute(val stride: Int, val size: Int) {
    class Coordinates2D(val points: Array<PointF>): Attribute(8, 24)
}

sealed class Uniform {
    class ColorUniform(val color: Color) : Uniform()
}
// A bit of interface segregation
interface Shape2DGLAttributesDescriptor {
    val attributes: Array<Attribute>
}

interface Shape2DGLUniformsDescriptor {
    val uniforms: Array<Uniform>
}

class MonochromaticTriangleGLDescriptor(val coords: Array<PointF>, val color: Color): Shape2DGLDescriptor, Shape2DGLAttributesDescriptor, Shape2DGLUniformsDescriptor{
    override val vShaderCode =
        "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}"

    override val fShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    override val attributes = arrayOf(Attribute.Coordinates2D(coords) as Attribute)

    override val uniforms = arrayOf(Uniform.ColorUniform(Color.DKGRAY.toColor()) as Uniform)

    override val transformationMatrix = floatArrayOf(1f, 1f, 1f, 1f)
}

class GLShape(val descriptor: Shape2DGLDescriptor) {
    private val coordsPerVertex = 2 // I cant see a way for sane abstract implementation here
    private val coordsByteSize = 4

    private val program: Int

    val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, descriptor.vShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, descriptor.fShaderCode)

        program = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    fun draw() {
        GLES20.glUseProgram(program)

        // get handle to vertex shader's vPosition member
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also {

            if(descriptor is Shape2DGLAttributesDescriptor) {
                GLES20.glEnableVertexAttribArray(it)

                val requiredBufferSize = descriptor.attributes.sumOf { it.size }

                val buffer = ByteBuffer.allocateDirect(requiredBufferSize).run {
                    order(ByteOrder.nativeOrder())
                }

                descriptor.attributes.forEach { attribute ->
                    if( attribute is Attribute.Coordinates2D) {
                        val coordinatesArray = FloatArray(attribute.points.size * 2)

                        attribute.points.forEachIndexed { idx, point ->
                            coordinatesArray[2 * idx] = point.x
                            coordinatesArray[2 * idx + 1] = point.y
                        }

                        buffer.asFloatBuffer().put(coordinatesArray)
                    }
                }

                GLES20.glVertexAttribPointer(
                    it,
                    coordsPerVertex,
                    GLES20.GL_FLOAT,
                    false,
                    coordsPerVertex * coordsByteSize,
                    buffer
                )

            }

            // get handle to fragment shader's vColor member

            GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}