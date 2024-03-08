package cafe.serenity.gl_es_practice

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColor
import cafe.serenity.gl_es_practice.databinding.ActivityMainBinding
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vShaderCode = loadShaderCodeFromAsset(TriangleDescriptor.vShaderAsset)
        val fShaderCode = loadShaderCodeFromAsset(TriangleDescriptor.fShaderAsset)

        val triangleDescriptor = TriangleDescriptor(
            vShaderCode,
            fShaderCode,
            floatArrayOf(
                -.5f, -.5f,
                .0f, .7f,
                .5f, -.5f))

        triangleDescriptor.setColor(Color.GRAY.toColor().components)

        binding.angleTxt.text = getString(R.string.degrees_txt, binding.slider.value)

        binding.slider.addOnChangeListener{ _, value, _ ->
            binding.angleTxt.text = getString(R.string.degrees_txt, value)
            triangleDescriptor.setRotation(value)
            cafe.serenity.descriptor_renderer.GPURenderer.updateDescriptor(triangleDescriptor)
        }

        binding.surfaceView.also {
            it.setEGLContextClientVersion(cafe.serenity.descriptor_renderer.EGL_CONTEXT_CLIENT_VERSION)
            it.setRenderer(object: Renderer{

                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                    assert(gl != null)
                    gl?.apply {
                        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
                    }
                    cafe.serenity.descriptor_renderer.GPURenderer.createProgram(triangleDescriptor)
                }

                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                    // Remove ???
                    GLES20.glViewport(0, 0, width, height)
                    gl?.apply {
                        glViewport(0, 0, width, height)

                        val ratio = width.toFloat() / height.toFloat()

                        glMatrixMode(GL10.GL_PROJECTION)                                   // set matrix to projection mode
                        glLoadIdentity()                                                   // reset the matrix to its default state
                        glFrustumf(-ratio, ratio, -1f, 1f, 3f, 7f)
                    }

                    if(height < width) {
                        triangleDescriptor.setScale(height.toFloat() / width.toFloat(), 1f)
                    } else {
                        triangleDescriptor.setScale(1f, width.toFloat() / height.toFloat())
                    }
                    cafe.serenity.descriptor_renderer.GPURenderer.updateDescriptor(triangleDescriptor)
                }

                override fun onDrawFrame(gl: GL10?) {
                    gl?.apply {
                        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    }
                    cafe.serenity.descriptor_renderer.GPURenderer.render()
                }
            })
        }
    }

    private fun loadShaderCodeFromAsset(shaderAsset: String): String{
        val stream = assets.open(shaderAsset)
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        return String(buffer)
    }
}