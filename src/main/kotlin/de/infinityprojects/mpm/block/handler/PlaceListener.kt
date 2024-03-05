package de.infinityprojects.mpm.block.handler

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.storage.Position
import de.infinityprojects.mpm.block.storage.Region
import de.infinityprojects.mpm.item.mutable.MPMItem
import de.infinityprojects.mpm.item.mutable.MutableItem
import de.infinityprojects.mpm.packet.PacketHandler
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class PlaceListener : Listener {
    val reg = Manager.getManager().getBlockRegistry()

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (e.action.isLeftClick) return

        val item = e.item ?: return
        val cBlock = e.clickedBlock ?: return

        if (cBlock.type.isInteractable && !e.player.isSneaking) return

        val blockFace = e.blockFace
        val block = cBlock.getRelative(blockFace)
        val mutItem = MutableItem.fromItemStack(item)
        if (mutItem is MPMItem) {
            val mpmItem = mutItem.item
            if (mpmItem is BlockProvider) {
                if (!block.canPlace(mpmItem.model.blockData)) return
                if (block.location.toCenterLocation().getNearbyEntities(0.5, 0.5, 0.5).isNotEmpty()) return

                val entity = (e.player as CraftPlayer).handle
                val animation = ClientboundAnimatePacket(entity, 0)

                Region.setBlockAt(e.player.world, Position(block.x, block.y, block.z), mpmItem)

                PacketHandler.sendPacket(e.player, animation)
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("mpm")!!,
                ) { _ ->
                    mpmItem.setBlock(block.location)
                    e.player.world.playSound(block.location, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f)
                }
                e.isCancelled = true
            }
        }
    }
}
