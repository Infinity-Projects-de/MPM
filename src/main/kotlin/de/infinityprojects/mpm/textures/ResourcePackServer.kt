package de.infinityprojects.mpm.textures

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

class ResourcePackServer(
    val file: ByteArray,
) {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(4120), 0)

    init {
        server.createContext("/", ResourcePackHandler())
        server.start()
    }

    inner class ResourcePackHandler : HttpHandler {
        override fun handle(t: HttpExchange) {
            t.responseHeaders.add("Content-Type", "application/zip")
            t.sendResponseHeaders(200, file.size.toLong())
            val os = t.responseBody
            os.write(file)
            os.close()
        }
    }

    fun stop() {
        server.stop(0)
    }
}
