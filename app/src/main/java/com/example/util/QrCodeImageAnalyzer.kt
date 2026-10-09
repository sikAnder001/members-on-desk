package com.example.util

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QrCodeImageAnalyzer(
    var isScanningEnabled: Boolean = true,
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    private var lastScannedTime = 0L

    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningEnabled) {
            imageProxy.close()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastScannedTime < 1500L) {
            imageProxy.close()
            return
        }

        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val yBytes = extractYPlaneWithStride(imageProxy)

            if (yBytes != null) {
                val origWidth = imageProxy.width
                val origHeight = imageProxy.height

                // Rotate luminance bytes to match display orientation
                val (rotatedData, finalWidth, finalHeight) = rotateYuv(
                    yBytes,
                    origWidth,
                    origHeight,
                    rotationDegrees
                )

                val source = PlanarYUVLuminanceSource(
                    rotatedData,
                    finalWidth,
                    finalHeight,
                    0,
                    0,
                    finalWidth,
                    finalHeight,
                    false
                )

                var decoded = decodeSource(source)

                // If not decoded, try with inverted luminance (black-on-white / white-on-black)
                if (decoded == null) {
                    val invertedSource = source.invert()
                    decoded = decodeSource(invertedSource)
                }

                // If still not decoded and rotation was 0 or 180, also try 90-degree rotated source just in case sensor orientation differed
                if (decoded == null && (rotationDegrees == 0 || rotationDegrees == 180)) {
                    val (altRotated, altW, altH) = rotateYuv(yBytes, origWidth, origHeight, 90)
                    val altSource = PlanarYUVLuminanceSource(
                        altRotated,
                        altW,
                        altH,
                        0,
                        0,
                        altW,
                        altH,
                        false
                    )
                    decoded = decodeSource(altSource)
                }

                if (!decoded.isNullOrBlank()) {
                    lastScannedTime = now
                    onQrCodeScanned(decoded.trim())
                }
            }
        } catch (e: Exception) {
            Log.e("QrCodeImageAnalyzer", "Error analyzing frame: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun decodeSource(source: LuminanceSource): String? {
        return try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            result?.text
        } catch (e: NotFoundException) {
            null
        } catch (e: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    /**
     * Extracts the Y (luminance) plane while correctly taking rowStride into account.
     * In Android CameraX / Camera2, rowStride can be greater than imageProxy.width.
     */
    private fun extractYPlaneWithStride(imageProxy: ImageProxy): ByteArray? {
        val planes = imageProxy.planes
        if (planes.isEmpty()) return null

        val yPlane = planes[0]
        val buffer: ByteBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        val yData = ByteArray(width * height)

        if (rowStride == width && pixelStride == 1) {
            buffer.rewind()
            val remaining = buffer.remaining().coerceAtMost(yData.size)
            buffer.get(yData, 0, remaining)
        } else {
            // Buffer has padding bytes at the end of each row
            buffer.rewind()
            val rowBuffer = ByteArray(rowStride)
            for (row in 0 until height) {
                val bytesToRead = rowStride.coerceAtMost(buffer.remaining())
                if (bytesToRead <= 0) break
                buffer.get(rowBuffer, 0, bytesToRead)
                if (pixelStride == 1) {
                    System.arraycopy(rowBuffer, 0, yData, row * width, width)
                } else {
                    for (col in 0 until width) {
                        yData[row * width + col] = rowBuffer[col * pixelStride]
                    }
                }
            }
        }
        return yData
    }

    /**
     * Rotates a 1-byte-per-pixel array (luminance data) by the given degrees (0, 90, 180, 270).
     */
    private fun rotateYuv(
        data: ByteArray,
        width: Int,
        height: Int,
        degrees: Int
    ): Triple<ByteArray, Int, Int> {
        when (degrees) {
            90 -> {
                val rotated = ByteArray(data.size)
                var destIndex = 0
                for (x in 0 until width) {
                    for (y in height - 1 downTo 0) {
                        rotated[destIndex++] = data[y * width + x]
                    }
                }
                return Triple(rotated, height, width)
            }
            180 -> {
                val rotated = ByteArray(data.size)
                val total = width * height
                for (i in 0 until total) {
                    rotated[i] = data[total - 1 - i]
                }
                return Triple(rotated, width, height)
            }
            270 -> {
                val rotated = ByteArray(data.size)
                var destIndex = 0
                for (x in width - 1 downTo 0) {
                    for (y in 0 until height) {
                        rotated[destIndex++] = data[y * width + x]
                    }
                }
                return Triple(rotated, height, width)
            }
            else -> {
                return Triple(data, width, height)
            }
        }
    }
}
