package com.gutearbyte.meetandy

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.Deferred
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PhotoArFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String?> {
        val additionalPermissions = super.getAdditionalPermissions()
        val permissionLength = additionalPermissions?.size ?: 0
        val permissions = arrayOfNulls<String>(permissionLength + 1)
        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (permissionLength > 0) {
            System.arraycopy(additionalPermissions!!, 0, permissions, 1, additionalPermissions.size)
        }
        return permissions
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString() + File.separator + "MeetAndy/" + date + ".jpg"
    }

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    suspend fun savePhoto() = withContext(Dispatchers.IO) {
        val filename = generateFilename()

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(
            arSceneView.width, arSceneView.height,
            Bitmap.Config.ARGB_8888
        )

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        return@withContext suspendCoroutine { cont: Continuation<Uri> ->
            async {
                PixelCopy.request(arSceneView, bitmap, { copyResult: Int ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        try {
                            saveBitmapToDisk(bitmap, filename)
                        } catch (e: IOException) {
                            val toast = Toast.makeText(
                                activity, e.toString(),
                                Toast.LENGTH_LONG
                            )
                            toast.show()
                        }

                        val photoFile = File(filename)
                        val photoURI = FileProvider.getUriForFile(
                            activity!!.applicationContext,
                            activity!!.packageName + ".meetandy.name.provider",
                            photoFile
                        )

                        cont.resume(photoURI)
                    } else {
                        // Failed to save photo
                        cont.resumeWithException(Exception("PixelCopy error code $copyResult"))
                    }
                    handlerThread.quitSafely()
                }, Handler(handlerThread.looper))
            }
        }
    }
}