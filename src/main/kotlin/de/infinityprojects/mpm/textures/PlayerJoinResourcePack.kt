package de.infinityprojects.mpm.textures

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

class PlayerJoinResourcePack(val hash: String) : Listener {
    private val serverAddress: String // TODO: Change to configurable IP?

    init {
        DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            val address = socket.localAddress.hostAddress
            if (address == "0.0.0.0") {
                val osXSocket: Socket = Socket()
                osXSocket.connect(InetSocketAddress("google.com", 80))
                serverAddress = osXSocket.localAddress.hostAddress
            } else {
                serverAddress = address
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val isLocal = e.player.address?.address?.isLoopbackAddress ?: false
        val address = if (isLocal) "127.0.0.1" else serverAddress

        val resourcePackInfo =
            ResourcePackInfo
                .resourcePackInfo()
                .uri(URI("http://$address:4120/#$hash"))
                .hash(hash).build()

        val request =
            ResourcePackRequest.resourcePackRequest()
                .packs(resourcePackInfo)
                .required(true)
                .build()

        e.player.sendResourcePacks(request)
    }
}
