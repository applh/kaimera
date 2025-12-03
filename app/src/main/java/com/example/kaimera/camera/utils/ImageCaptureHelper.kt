package com.example.kaimera.camera.utils

import com.example.kaimera.camera.managers.StorageManager
import com.example.kaimera.camera.utils.ExifUtils



import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

/**
 * Utility class for handling image capture in different formats (JPEG, WebP).
 */
object ImageCaptureHelper {

    private const val TAG = "ImageCaptureHelper"

    /**
     * Capture image in the specified format
     */
    fun captureImage(
        imageCapture: ImageCapture,
        outputFile: File,
        format: String,
        quality: Int,
        location: android.location.Location? = null,
        executor: Executor,
        onSuccess: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        when (format) {
            "webp" -> captureAsWebP(imageCapture, outputFile, quality, location, executor, onSuccess, onError)
            else -> captureAsJPEG(imageCapture, outputFile, location, executor, onSuccess, onError)
        }
    }

    /**
     * Capture image as JPEG using file-based output
     */
    private fun captureAsJPEG(
        imageCapture: ImageCapture,
        outputFile: File,
        location: android.location.Location?,
        executor: Executor,
        onSuccess: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val metadata = ImageCapture.Metadata().apply {
            this.location = location
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSuccess(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * Capture image as WebP using memory-based output
     */
    private fun captureAsWebP(
        imageCapture: ImageCapture,
        outputFile: File,
        quality: Int,
        location: android.location.Location?,
        executor: Executor,
        onSuccess: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        // Get JPEG bytes to extract EXIF
                        val jpegBytes = imageProxyToBytes(imageProxy)
                        
                        // Create source EXIF from bytes
                        val tempFile = File.createTempFile("temp_exif", ".jpg")
                        FileOutputStream(tempFile).use { it.write(jpegBytes) }
                        
                        val sourceExif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
                        
                        // Decode Bitmap
                        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                        
                        // Save as WebP
                        saveBitmapAsWebP(bitmap, outputFile, quality)
                        
                        // Copy EXIF to new WebP file
                        val targetExif = androidx.exifinterface.media.ExifInterface(outputFile.absolutePath)
                        ExifUtils.copyExif(sourceExif, targetExif)
                        
                        // Add location if provided
                        if (location != null) {
                            targetExif.setGpsInfo(location)
                            targetExif.saveAttributes()
                        }
                        
                        // Clean up
                        bitmap.recycle()
                        tempFile.delete()
                        imageProxy.close()
                        
                        onSuccess(outputFile)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save WebP image", e)
                        imageProxy.close()
                        onError(ImageCaptureException(
                            ImageCapture.ERROR_FILE_IO,
                            "Failed to save WebP: ${e.message}",
                            e
                        ))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * Convert ImageProxy to ByteArray (JPEG)
     */
    private fun imageProxyToBytes(imageProxy: ImageProxy): ByteArray {
        if (imageProxy.format == ImageFormat.JPEG) {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return bytes
        }

        val image = imageProxy.image ?: throw IllegalStateException("Image is null")
        
        // Convert YUV to RGB
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        return out.toByteArray()
    }

    /**
     * Save Bitmap as WebP with appropriate format based on API level
     */
    private fun saveBitmapAsWebP(bitmap: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use { out ->
            val compressFormat = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // API 30+: Use WEBP_LOSSY
                    Bitmap.CompressFormat.WEBP_LOSSY
                }
                else -> {
                    // API 24-29: Use deprecated WEBP
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            
            bitmap.compress(compressFormat, quality, out)
            out.flush()
        }
    }

    /**
     * Get file extension for the given format
     */
    fun getFileExtension(format: String): String {
        return when (format) {
            "webp" -> "webp"
            else -> "jpg"
        }
    }
}
