package de.infinityprojects.mpm.block.handler

import de.infinityprojects.mpm.Main
import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.storage.Position
import de.infinityprojects.mpm.block.storage.Region
import de.infinityprojects.mpm.packet.PacketEvent
import de.infinityprojects.mpm.packet.PacketListener
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData

class BlockHandler(val mpm: Main) {
    val reg = Manager.getManager().getBlockRegistry()

    @PacketListener
    fun blockSetPacket(e: PacketEvent<ClientboundBlockUpdatePacket>) {
        val p = e.packet
        val player = e.player
        val pos = p.pos

        val block = Region.getBlockAt(player.world, Position(pos.x, pos.y, pos.z))

        if (block != null && block is BlockProvider) {
            val loc = Location(player.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

            val state = (block.model.blockData as CraftBlockData).state

            e.packet = ClientboundBlockUpdatePacket(pos, state)
        }
    }

    fun destroyBlock(
        world: World,
        pos: Position,
        drop: Boolean,
    ) {
        Bukkit.getScheduler().runTask(mpm) { _ ->
            val loc = world.getBlockAt(pos.x, pos.y, pos.z).location
            loc.block.type = Material.AIR
        }
        if (drop) {
            // drop item
        }
        Region.setBlockAt(world, pos, null)
    }

    @PacketListener
    fun onStartBreak(e: PacketEvent<ServerboundPlayerActionPacket>) {
        val p = e.packet
        val pos = p.pos
        val player = e.player
        if (p.action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            if (player.gameMode == GameMode.CREATIVE) {
                destroyBlock(player.world, Position(pos.x, pos.y, pos.z), false)
            } else {
                val block = Region.getBlockAt(player.world, pos.x, pos.y, pos.z)
                if (block != null) {
                    BreakHandler.startBreaking(
                        player,
                        Location(player.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()),
                        Region.getBlockAt(player.world, Position(pos.x, pos.y, pos.z)) as BlockProvider,
                        player.inventory.itemInMainHand,
                    )
                }
            }
        } else if (p.action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
            p.action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
        ) {
            BreakHandler.stopBreaking(player)
        } else {
            return
        }
    }
}
