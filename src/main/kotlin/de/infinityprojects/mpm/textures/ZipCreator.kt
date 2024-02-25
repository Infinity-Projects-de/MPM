package de.infinityprojects.mpm.textures

import de.infinityprojects.mpm.Main
import org.bukkit.plugin.Plugin
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class ZipCreator(
    val mpm: Plugin
) : Closeable {
    private val out = ByteArrayOutputStream()
    private val zip: ZipOutputStream = ZipOutputStream(out)

    private var result: ByteArray = ByteArray(0)

    init {
        if (mpm !is Main) {
            throw IllegalArgumentException("Plugin is not MPM")
        }

        zip.setComment("MPM Texture Pack")
    }

    fun addFile(file: File, path: String) {
        val e = ZipEntry(path)
        zip.putNextEntry(e)
        Files.copy(file.toPath(), zip)
        zip.closeEntry()
    }

    fun writeData(file: ByteArrayOutputStream, path: String) {
        val e = ZipEntry(path)
        zip.putNextEntry(e)
        file.writeTo(zip)
        zip.closeEntry()
    }

    fun writeData(file: ByteArray, path: String) {
        val e = ZipEntry(path)
        zip.putNextEntry(e)
        zip.write(file)
        zip.closeEntry()
    }

    fun writeStream(file: InputStream, path: String) {
        val e = ZipEntry(path)
        zip.putNextEntry(e)
        file.copyTo(zip)
        zip.closeEntry()
    }

    fun getBytes(): ByteArray {
        return result
    }

    fun getSha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val digestStream = DigestInputStream(getBytes().inputStream(), digest)
        val buffer = ByteArray(1024)
        while (digestStream.read(buffer) > 0);
        val sha1 = digest.digest()
        val sha1string = sha1.joinToString("") {
            String.format("%02x", it)
        }
        digestStream.close()
        println(sha1string)
        println(sha1string.length)
        println(sha1)
        println(sha1.size)
        return sha1string
    }

    override fun close() {
        zip.close()
        mpm.dataFolder.mkdirs()
        result = out.toByteArray()
        Files.write(mpm.dataFolder.resolve("textures.zip").toPath(), out.toByteArray())
        out.close()
    }


}