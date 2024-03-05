package cafe.serenity.gl_es_practice

import android.graphics.Color
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColor
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import cafe.serenity.gl_es_practice.databinding.ActivityMainBinding
import com.google.android.material.slider.RangeSlider

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lateinit var fShaderCode: String
        lateinit var vShaderCode: String

        try {
            val stream = assets.open("triangle_vertex_shader.glsl")
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            vShaderCode = String(buffer)
        } catch (e: IOException) {
            // Log it
        }

        try {
            val stream = assets.open("triangle_fragment_shader.glsl")
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            fShaderCode = String(buffer)
        } catch (e: IOException) {
            // Log it
        }

        val triangleDescriptor = MonochromaticTriangleGLDescriptor(
            vShaderCode,
            fShaderCode,
            arrayOf(
                PointF(-.5f, -.5f),
                PointF(.0f, .7f),
                PointF(.5f, -.5f)),
            Color.DKGRAY.toColor())

        binding.slider.addOnChangeListener{ _, value, _ ->
            triangleDescriptor.setRotation(value)
        }

        binding.surfaceView.also {
            it.setEGLContextClientVersion(2)
            it.setRenderer(object: Renderer{
                private lateinit var triangle: GLShape
                private var ratio: Float = 0f
                private lateinit var triangleDescriptor: MonochromaticTriangleGLDescriptor

                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                    assert(gl != null)
                    gl?.apply {
                        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
                    }
                    triangle = GLShape(triangleDescriptor)
                    triangle.descriptor
                }

                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                    GLES20.glViewport(0, 0, width, height)
                    gl?.apply {
                        glViewport(0, 0, width, height)

                        ratio = width.toFloat() / height.toFloat()

                        glMatrixMode(GL10.GL_PROJECTION)                                   // set matrix to projection mode
                        glLoadIdentity()                                                   // reset the matrix to its default state
                        glFrustumf(-ratio, ratio, -1f, 1f, 3f, 7f)  // apply the projection matrix
                    }
                }

                override fun onDrawFrame(gl: GL10?) {
                    gl?.apply {
                        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    }
                    if(ratio > 1f) {
                        triangle.draw(1f / ratio, 1f)
                    } else {
                        triangle.draw(1f, ratio)
                    }
                }
            })
        }
    }
}

interface Shape2DGLDescriptor {
    val vShaderCode: String
    val fShaderCode: String
}


sealed class Attribute(val stride: Int, val size: Int) {
    class Coordinates2D(val points: Array<PointF>): Attribute(8, 24)
}

sealed class Uniform(val size: Int) {
    class ColorUniform(val color: Color) : Uniform(4 * 4)
    class ProjectionMatrixUniform(val rotationMatrixArray: FloatArray): Uniform(9 * 4)
}
// A bit of interface segregation
interface Shape2DGLAttributesDescriptor {
    val attributes: Array<Attribute>
}

interface Shape2DGLUniformsDescriptor {
    val uniforms: Array<Uniform>
}

class MonochromaticTriangleGLDescriptor(override val vShaderCode: String, override val fShaderCode: String, coords: Array<PointF>, val color: Color): Shape2DGLDescriptor, Shape2DGLAttributesDescriptor, Shape2DGLUniformsDescriptor{
    override val attributes = arrayOf(Attribute.Coordinates2D(coords) as Attribute)

    private var transformationMatrix = floatArrayOf(
        1f, 0f,
        0f, 1f,
    )

    override val uniforms get() = arrayOf(
        Uniform.ProjectionMatrixUniform(transformationMatrix) as Uniform,
        Uniform.ColorUniform(color) as Uniform,
    )

    fun setRotation(degrees: Float) {
        val radians = ( PI.toFloat() * degrees )/ 180f

        transformationMatrix = floatArrayOf(
            cos(radians), -sin(radians),
            sin(radians), cos(radians),
        )
    }
}

class GLShape(val descriptor: Shape2DGLDescriptor) {
    private val coordsPerVertex = 2 // I cant see a way for sane abstract implementation here
    private val coordsByteSize = 4

    private val program: Int

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, descriptor.vShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, descriptor.fShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    fun draw(scaleX: Float, scaleY: Float) {
        GLES20.glUseProgram(program)

        // get handle to vertex shader's vPosition member
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also { attribLocation ->

            if(descriptor is Shape2DGLAttributesDescriptor) {
                GLES20.glEnableVertexAttribArray(attribLocation)

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
                    attribLocation,
                    coordsPerVertex,
                    GLES20.GL_FLOAT,
                    false,
                    8,
                    buffer
                )

            }

            if ( descriptor is Shape2DGLUniformsDescriptor ) {
                descriptor.uniforms.forEach { uniform ->
                    if( uniform is Uniform.ColorUniform ) {
                        GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
                            GLES20.glUniform4fv(colorHandle, 1, uniform.color.components, 0)
                        }


                    } else if (uniform is Uniform.ProjectionMatrixUniform) {
                        GLES20.glGetUniformLocation(program, "rotationMatrix").also {rotationMatrixHandle ->
                            GLES20.glUniformMatrix2fv(rotationMatrixHandle, 1, false, uniform.rotationMatrixArray, 0)
                        }
                    }
                }
            }

            GLES20.glGetUniformLocation(program, "scaleX").also {scaleXHandle ->
                GLES20.glUniform1f(scaleXHandle, scaleX)
            }

            GLES20.glGetUniformLocation(program, "scaleY").also {scaleYHandle ->
                GLES20.glUniform1f(scaleYHandle, scaleY)
            }

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(attribLocation)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}