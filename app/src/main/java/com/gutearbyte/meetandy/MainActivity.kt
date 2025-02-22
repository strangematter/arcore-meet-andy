package com.gutearbyte.meetandy

import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var renderable: Renderable
    private lateinit var arFragment: PhotoArFragment
    private var node: TransformableNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = ux_fragment as PhotoArFragment

        ModelRenderable.builder()
            .setSource(this, Uri.parse("Andy.sfb"))
            .build()
            .thenAccept { this.renderable = it }
            .exceptionally {
                val toast = Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }

        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            // Create the Anchor.
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            // Create the transformable andy and add it to the anchor.
            val modelNode = TransformableNode(arFragment.transformationSystem)
            modelNode.setParent(anchorNode)
            modelNode.renderable = renderable
            modelNode.scaleController.maxScale = 5.00f;
            modelNode.scaleController.minScale = 0.01f;
            node = modelNode
            modelNode.select()
        }

        fabTakePicture.setOnClickListener { takePhotoWithCountdown() }
    }

    private fun takePhotoWithCountdown() {
        object : CountDownTimer(10000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = ((millisUntilFinished / 1000) + 1).toString()
            }

            override fun onFinish() {
                tvCountdown.text = getString(R.string.cheese)
                tvCountdown.postDelayed({
                    tvCountdown.visibility = View.INVISIBLE
                    fabTakePicture.show()
                    arFragment.arSceneView.planeRenderer.isEnabled = true
                }, 1000)
                arFragment.takePhoto()
            }
        }.start()
        // Hide all the chrome for a nice photo
        arFragment.arSceneView.planeRenderer.isEnabled = false
        tvCountdown.visibility = View.VISIBLE
        fabTakePicture.hide()
        node?.transformationSystem?.selectNode(null)
    }
}