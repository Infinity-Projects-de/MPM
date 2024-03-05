package de.infinityprojects.mpm.block.handler

import de.infinityprojects.mpm.block.storage.Position
import de.infinityprojects.mpm.block.storage.Region
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerInteractEvent

class NoteblockDisabler : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onNoteblockChange(e: BlockRedstoneEvent) {
        if (e.block.type == Material.NOTE_BLOCK) {
            e.newCurrent = e.oldCurrent
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onPlayerInteract(e: PlayerInteractEvent) {
        val clickedBlock = e.clickedBlock

        if (clickedBlock != null && e.action.isRightClick) {
            if (clickedBlock.type == Material.NOTE_BLOCK) {
                e.isCancelled = true
            }
            val block = Region.getBlockAt(e.player.world, Position(clickedBlock.x, clickedBlock.y, clickedBlock.z))
            if (block != null) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onAnimation(e: PlayerAnimationEvent) {
        e.isCancelled = true // TODO: Cancel interact at event
    }

    // prevent a player from playing a note
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onNotePlay(event: NotePlayEvent) {
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        val aboveBlock = event.block.getRelative(BlockFace.UP)
        if (aboveBlock.type == Material.NOTE_BLOCK) event.isCancelled = true
        if (event.block.type == Material.NOTE_BLOCK) event.isCancelled = true
    }
}
