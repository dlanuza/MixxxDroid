package com.dlanuza.mixxxdroid.player

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

internal object AudioEngineWaveformCache {

    private const val MAGIC = 0x4D495857 // "MIXW"
    private const val VERSION = 1

    private fun cacheDirectory(context: Context): File =
        File(context.cacheDir, "waveforms").also { if (!it.exists()) it.mkdirs() }

    private fun cacheFile(context: Context, uriString: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(uriString.toByteArray(Charsets.UTF_8))
        val hexKey = digest.joinToString("") { "%02x".format(it) }
        return File(cacheDirectory(context), "$hexKey.waveform")
    }

    fun load(context: Context, uriString: String): FloatArray? {
        val file = cacheFile(context, uriString)
        if (!file.isFile || file.length() < 12) return null

        return runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { dis ->
                if (dis.readInt() != MAGIC || dis.readInt() != VERSION) return null

                val count = dis.readInt()
                if (count <= 0 || count > BackgroundWaveformExtractor.TARGET_SAMPLES) return null

                val samples = FloatArray(count)
                val buffer = ByteArray(count * 4)
                dis.readFully(buffer)

                ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get(samples)
                samples
            }
        }.getOrElse {
            runCatching { file.delete() }
            null
        }
    }

    fun save(context: Context, uriString: String, samples: FloatArray) {
        if (samples.isEmpty()) return
        val file = cacheFile(context, uriString)
        val tempFile = File(file.parentFile, "${file.name}.tmp")

        runCatching {
            val byteBuffer = ByteBuffer.allocate(samples.size * 4).order(ByteOrder.BIG_ENDIAN)
            byteBuffer.asFloatBuffer().put(samples)

            DataOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { dos ->
                dos.writeInt(MAGIC)
                dos.writeInt(VERSION)
                dos.writeInt(samples.size)
                dos.write(byteBuffer.array())
            }

            if (file.exists()) file.delete()
            if (!tempFile.renameTo(file)) {
                tempFile.delete() // Limpieza si falla el rename de archivos
            }
        }.onFailure {
            runCatching { tempFile.delete() }
        }
    }

    fun delete(context: Context, uriString: String) {
        runCatching { cacheFile(context, uriString).delete() }
    }

    fun clear(context: Context) {
        runCatching { cacheDirectory(context).listFiles()?.forEach { it.delete() } }
    }
}