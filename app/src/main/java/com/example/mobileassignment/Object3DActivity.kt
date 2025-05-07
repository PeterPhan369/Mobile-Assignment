package com.example.mobileassignment

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class Object3DActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: VaseRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_3d_object)

        // 1) Setup GLSurfaceView & Renderer
        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            renderer = VaseRenderer().also {
                it.rotationX = 0f
                it.rotationY = 0f
                it.zoom = -5f
            }
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        // 2) Add GL view behind the buttons
        findViewById<FrameLayout>(R.id.model_container)
            .addView(glView, 0,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

        // 3) Hook up buttons:

        // Rotate: add 30° each tap around Y axis
        findViewById<Button>(R.id.btn_rotate).setOnClickListener {
            renderer.rotationY = (renderer.rotationY + 30f) % 360f
        }

        // Zoom In: move camera 0.5f closer (but not past -2f)
        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener {
            renderer.zoom = (renderer.zoom + 0.5f).coerceAtMost(-2f)
        }

        // Zoom Out: move camera 0.5f farther (but not beyond -20f)
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener {
            renderer.zoom = (renderer.zoom - 0.5f).coerceAtLeast(-20f)
        }

        // Back
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        glView.onPause()
        super.onPause()
    }
}
