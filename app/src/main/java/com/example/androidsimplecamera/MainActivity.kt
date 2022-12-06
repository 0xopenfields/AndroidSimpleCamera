package com.example.androidsimplecamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var cameraDevice: CameraDevice? = null

    private var imageSize: Size? = null

    private val textureView: TextureView by lazy {
        findViewById(R.id.textureView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView.surfaceTextureListener = surfaceTextureListener
    }

    override fun onResume() {
        super.onResume()

        if (textureView.isAvailable) {
            openCamera()
        }
    }

    override fun onPause() {
        super.onPause()

        cameraDevice?.close()
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera

            textureView.surfaceTexture!!.setDefaultBufferSize(imageSize!!.width, imageSize!!.height)

            val surface = Surface(textureView.surfaceTexture!!)
            val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequest.addTarget(surface)
            cameraDevice!!.createCaptureSession(
                SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                Collections.singletonList(OutputConfiguration(surface)),
                applicationContext.mainExecutor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        try {
                            session.setRepeatingRequest(captureRequest.build(), null, null)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@MainActivity, "Configuration change", Toast.LENGTH_SHORT).show()
                    }
                })
            )
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

            try {
                cameraManager.cameraIdList.forEachIndexed { index, _ ->
                    val cameraId = cameraManager.cameraIdList[index]
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        imageSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0]

                        cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null)
                    }
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        } else {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), 0)
        }
    }
}