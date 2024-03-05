package de.infinityprojects.mpm.block.handler

import de.infinityprojects.mpm.block.storage.Region
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class BlockRenderer : Listener {
    @EventHandler
    fun onPlayerChunkLoad(e: PlayerChunkLoadEvent) {
        val region = Region.getRegion(e.world, e.chunk.x, e.chunk.z)
        region.blocks.forEach { (pos, block) ->
            block as BlockProvider
            val loc = e.chunk.getBlock(0, 0, 0).location.add(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            e.player.sendBlockChange(loc, block.model.blockData)
        }
    } // TODO use section packet?
}
