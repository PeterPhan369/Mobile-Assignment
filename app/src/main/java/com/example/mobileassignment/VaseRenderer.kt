package com.example.mobileassignment

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VaseRenderer : GLSurfaceView.Renderer {
    private val mMVPMatrix    = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix   = FloatArray(16)
    private val mModelMatrix  = FloatArray(16)

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var normalBuffer:  FloatBuffer
    private lateinit var colorBuffer:   FloatBuffer

    private var program: Int = 0
    private var positionHandle:    Int = 0
    private var normalHandle:      Int = 0
    private var colorHandle:       Int = 0
    private var mvpMatrixHandle:   Int = 0
    private var modelMatrixHandle: Int = 0
    private var lightPosHandle:    Int = 0

    /** Exposed so Activity can adjust them */
    var rotationX: Float = 0f
    var rotationY: Float = 0f
    var zoom:      Float = -5.0f

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        generateVaseGeometry()

        // compile & link shaders
        val vs = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec4 vPosition;
            attribute vec3 vNormal;
            attribute vec4 vColor;
            uniform vec3 uLightPos;
            varying vec4 fColor;
            void main() {
              gl_Position = uMVPMatrix * vPosition;
              vec3 n = normalize(vec3(uModelMatrix * vec4(vNormal,0.0)));
              vec3 v = vec3(uModelMatrix * vPosition);
              vec3 L = normalize(uLightPos - v);
              float diff = max(dot(n,L),0.1);
              fColor = vec4(vColor.rgb * diff, vColor.a);
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            varying vec4 fColor;
            void main() {
              gl_FragColor = fColor;
            }
        """.trimIndent()

        val vShader = loadShader(GLES20.GL_VERTEX_SHADER,   vs)
        val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vShader)
            GLES20.glAttachShader(it, fShader)
            GLES20.glLinkProgram( it )
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0,0,width,height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(mProjectionMatrix, 0,
            -ratio, ratio,
            -1f, 1f,
            3f, 20f)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // camera
        Matrix.setLookAtM(mViewMatrix,0,
            0f,0f,zoom,
            0f,0f,0f,
            0f,1f,0f)

        // model
        Matrix.setIdentityM(mModelMatrix,0)
        Matrix.rotateM(mModelMatrix,0, rotationX, 1f,0f,0f)
        Matrix.rotateM(mModelMatrix,0, rotationY, 0f,1f,0f)
        // auto-spin
        val auto = (SystemClock.uptimeMillis() % 4000L)/4000f * 360f
        Matrix.rotateM(mModelMatrix,0, auto, 0f,1f,0f)

        // MVP
        Matrix.multiplyMM(mMVPMatrix,0, mViewMatrix,0, mModelMatrix,0)
        Matrix.multiplyMM(mMVPMatrix,0, mProjectionMatrix,0, mMVPMatrix,0)

        GLES20.glUseProgram(program)

        // attributes
        positionHandle = GLES20.glGetAttribLocation(program,"vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle,3,
            GLES20.GL_FLOAT,false,0,vertexBuffer)

        normalHandle = GLES20.glGetAttribLocation(program,"vNormal")
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle,3,
            GLES20.GL_FLOAT,false,0,normalBuffer)

        colorHandle = GLES20.glGetAttribLocation(program,"vColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle,4,
            GLES20.GL_FLOAT,false,0,colorBuffer)

        // uniforms
        mvpMatrixHandle   = GLES20.glGetUniformLocation(program,"uMVPMatrix")
        modelMatrixHandle = GLES20.glGetUniformLocation(program,"uModelMatrix")
        lightPosHandle    = GLES20.glGetUniformLocation(program,"uLightPos")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle,1,false,mMVPMatrix,0)
        GLES20.glUniformMatrix4fv(modelMatrixHandle,1,false,mModelMatrix,0)
        GLES20.glUniform3f(lightPosHandle, 5f,5f,zoom)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0, vertexBuffer.capacity()/3)

        // cleanup
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun loadShader(type:Int, src:String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader,src)
            GLES20.glCompileShader(shader)
        }
    }

    /** Programmatically generate vase mesh, normals & colors */
    private fun generateVaseGeometry() {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        val cols  = ArrayList<Float>()

        val stacks = 20; val slices = 20
        val baseR=0.5f; val topR=0.3f; val neckR=0.15f
        val h=1.5f; val neckH=0.3f; val baseH=0.1f

        fun radius(t:Float):Float {
            return when {
                t<baseH -> baseR
                t<0.5f -> baseR + (t-baseH)/(0.5f-baseH)*(topR-baseR)
                t<1f-neckH -> topR - (t-0.5f)/(0.5f-neckH)*(topR-neckR)
                else -> neckR
            }
        }

        fun addTri(x1:Float,y1:Float,z1:Float,
                   x2:Float,y2:Float,z2:Float,
                   x3:Float,y3:Float,z3:Float,
                   ht:Float, ang:Float) {
            verts += listOf(x1,y1,z1, x2,y2,z2, x3,y3,z3)
            // face normal
            val nx=(y2-y1)*(z3-z1)-(z2-z1)*(y3-y1)
            val ny=(z2-z1)*(x3-x1)-(x2-x1)*(z3-z1)
            val nz=(x2-x1)*(y3-y1)-(y2-y1)*(x3-x1)
            val len = sqrt(nx*nx+ny*ny+nz*nz)
            val (nxf,nyf,nzf) = if(len>0f) listOf(nx/len,ny/len,nz/len) else listOf(0f,0f,1f)
            repeat(3) { norms += listOf(nxf,nyf,nzf) }
            val r = 0.2f + 0.6f*ht
            val g = 0.1f + 0.5f*ht
            val b = 0.7f - 0.5f*ht
            val v = 0.05f * sin(ang*6f*PI.toFloat())
            repeat(3) { cols += listOf(r+v,g+v,b-v,1f) }
        }

        for(s in 0 until stacks) {
            val t = s.toFloat()/(stacks-1)
            val nt= (s+1).toFloat()/(stacks-1)
            val z = -h/2 + t*h
            val nz= -h/2 + nt*h
            val r = radius(t)
            val nr= radius(nt)
            for(i in 0 until slices) {
                val a = i.toFloat()/slices * 2f*PI.toFloat()
                val na= (i+1).toFloat()/slices*2f*PI.toFloat()
                val x1=r*cos(a);      val y1=r*sin(a)
                val x2=r*cos(na);     val y2=r*sin(na)
                val x3=nr*cos(na);    val y3=nr*sin(na)
                val x4=nr*cos(a);     val y4=nr*sin(a)
                addTri(x1,y1,z, x2,y2,z, x3,y3,nz, t, i.toFloat()/slices)
                addTri(x1,y1,z, x3,y3,nz,x4,y4,nz, t, i.toFloat()/slices)
            }
        }

        // convert to FloatBuffer
        fun toBuffer(list: List<Float>): FloatBuffer {
            return ByteBuffer
                .allocateDirect(list.size*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(list.toFloatArray()); position(0) }
        }

        vertexBuffer = toBuffer(verts)
        normalBuffer = toBuffer(norms)
        colorBuffer  = toBuffer(cols)
    }
}
