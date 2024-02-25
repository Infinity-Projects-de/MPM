package de.infinityprojects.mpm.textures

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.net.URI

class PlayerJoinResourcePack(val hash: String): Listener {

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val resourcePackInfo = ResourcePackInfo
            .resourcePackInfo()
            .uri(URI("http://127.0.0.1:4120/#$hash")) // TODO: Change to IP?
            .computeHashAndBuild().get()

        val request = ResourcePackRequest.resourcePackRequest()
            .packs(resourcePackInfo)
            .required(true)
            .build()

        e.player.sendResourcePacks(request)
    }
}