package de.infinityprojects.mpm.block

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.packet.PacketEvent
import de.infinityprojects.mpm.packet.PacketListener
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.World
import org.bukkit.block.Barrel
import org.bukkit.persistence.PersistentDataType

class BlockHandler {
    @PacketListener
    fun blockSetPacket(e: PacketEvent<ClientboundBlockUpdatePacket>) {
        val p = e.packet

        val block = getRealBlock(p.pos, e.player.world, p.blockState)
        if (block != p.blockState) {
            e.packet = ClientboundBlockUpdatePacket(p.pos, block)
        }
    }

    private fun getRealBlock(pos: BlockPos, world: World, original: BlockState): BlockState {
        val block = world.getBlockAt(pos.x, pos.y, pos.z)
        if (block is Barrel) {
            val tags = block.persistentDataContainer
            tags.keys.forEach {
                val value = tags.get(it, PersistentDataType.STRING)
                if (value == "mpm-block") {
                    val mBlock = Manager.getManager().getBlockRegistry().get(it)
                    mBlock as BlockProvider
                    
                    return mBlock.model.blockState;
                }
            }
        }
        return original
    }
}