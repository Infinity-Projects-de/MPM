package de.infinityprojects.mpm.block.handler

import de.infinityprojects.mpm.block.storage.Region
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

class BreakListener : Listener {
    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val block = Region.getBlockAt(e.block.world, e.block.x, e.block.y, e.block.z)
        if (block != null) {
            e.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onBlockInteract(e: PlayerInteractEvent) {
        if (e.action.isRightClick) return
        val cBlock = e.clickedBlock
        if (cBlock != null) {
            val block = Region.getBlockAt(e.player.world, cBlock.x, cBlock.y, cBlock.z)

            if (block != null) {
                if (e.player.gameMode == GameMode.CREATIVE) {
                    Region.setBlockAt(cBlock.world, cBlock.x, cBlock.y, cBlock.z, null)
                } else {
                    e.isCancelled = true
                }
            }
        }
    }
}
