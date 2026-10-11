package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

object ResumeParserUtils {

    fun extractTextFromUri(context: Context, uri: Uri, mimeType: String?): String? {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                when {
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || 
                    uri.path?.endsWith(".docx", ignoreCase = true) == true -> {
                        val result = extractTextFromDocx(inputStream)
                        if (result.trim().isEmpty()) {
                            extractPrintableTextCompiled(context, uri)
                        } else {
                            result
                        }
                    }
                    mimeType == "text/plain" || uri.path?.endsWith(".txt", ignoreCase = true) == true -> {
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
                    else -> {
                        val backup = extractPrintableTextCompiled(context, uri)
                        if (backup.trim().isNotEmpty()) backup else null
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            try {
                val backup = extractPrintableTextCompiled(context, uri)
                if (backup.trim().isNotEmpty()) backup else null
            } catch (ex: Throwable) {
                null
            }
        }
    }

    private fun extractTextFromDocx(inputStream: InputStream): String {
        return try {
            ZipInputStream(inputStream).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                val textBuilder = StringBuilder()
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val content = zipInputStream.bufferedReader().readText()
                        val matcher = Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(content)
                        while (matcher.find()) {
                            val rawText = matcher.group(1)
                            if (rawText != null) {
                                textBuilder.append(rawText).append(" ")
                            }
                        }
                        break
                    }
                    entry = zipInputStream.nextEntry
                }
                textBuilder.toString()
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .trim()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            ""
        }
    }

    fun getUriBytes(context: Context, uri: Uri): ByteArray {
        val maxAllowedSize = 15 * 1024 * 1024 // 15MB maximum file size protection
        var sizeOfFile: Long = 0
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    sizeOfFile = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (sizeOfFile > maxAllowedSize) {
            throw Exception("The uploaded document exceeds the 15MB limit. Please crop or compress your resume file.")
        }

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Storage streams not accessible. Make sure the file exists and is accessible.")
        inputStream.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.isEmpty()) {
                throw Exception("Selected file is entirely empty.")
            }
            return bytes
        }
    }

    fun preprocessBitmapForOcr(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        return try {
            val output = android.graphics.Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint()
            
            // Adjust contrast to 1.35x and enhance black ink readability for poor mobile scans
            val contrastValue = 1.35f
            val brightnessValue = 5.0f
            val array = floatArrayOf(
                contrastValue, 0f, 0f, 0f, brightnessValue,
                0f, contrastValue, 0f, 0f, brightnessValue,
                0f, 0f, contrastValue, 0f, brightnessValue,
                0f, 0f, 0f, 1f, 0f
            )
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(array))
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            output
        } catch (e: Throwable) {
            e.printStackTrace()
            bitmap
        }
    }

    fun preprocessImageBytes(bytes: ByteArray): ByteArray {
        var bitmap: android.graphics.Bitmap? = null
        var preprocessed: android.graphics.Bitmap? = null
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            // Scale down excessively large images to max 2048px to safeguard JVM heap allocations
            var inSampleSize = 1
            val maxDimension = 2048
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxDimension || halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }
            
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            
            bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (bitmap != null) {
                preprocessed = preprocessBitmapForOcr(bitmap)
                val stream = java.io.ByteArrayOutputStream()
                preprocessed.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
                val outBytes = stream.toByteArray()
                
                if (bitmap !== preprocessed) {
                    bitmap.recycle()
                }
                preprocessed.recycle()
                
                outBytes
            } else {
                bytes
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            try {
                if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
                if (preprocessed != null && !preprocessed.isRecycled) preprocessed.recycle()
            } catch (ignored: Throwable) {}
            bytes
        }
    }

    fun bytesToBase64(bytes: ByteArray): String {
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Selected Resume"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    fun extractPrintableTextCompiled(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { br ->
                    val sb = java.lang.StringBuilder()
                    var line = br.readLine()
                    var linesRead = 0
                    while (line != null && linesRead < 3000) {
                        val cleaned = line.replace(Regex("[^\\p{Print}\\s]"), "")
                        if (cleaned.isNotBlank()) {
                            sb.append(cleaned).append("\n")
                        }
                        line = br.readLine()
                        linesRead++
                    }
                    sb.toString().trim()
                }
            } ?: ""
        } catch (e: Throwable) {
            e.printStackTrace()
            ""
        }
    }

    fun renderPdfToImages(context: Context, uri: Uri, maxPages: Int = 3): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("pdf_render_temp", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            if (fileDescriptor != null) {
                renderer = PdfRenderer(fileDescriptor)
                val pageCount = renderer.pageCount.coerceAtMost(maxPages)
                for (i in 0 until pageCount) {
                    var page: PdfRenderer.Page? = null
                    var bitmap: Bitmap? = null
                    try {
                        page = renderer.openPage(i)
                        val width = (page.width * 1.5).toInt()
                        val height = (page.height * 1.5).toInt()
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        result.add(stream.toByteArray())
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        bitmap?.recycle()
                        page?.close()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
            } catch (ignored: Throwable) {}
            try {
                fileDescriptor?.close()
            } catch (ignored: Throwable) {}
            try {
                tempFile?.delete()
            } catch (ignored: Throwable) {}
        }
        return result
    }
}
