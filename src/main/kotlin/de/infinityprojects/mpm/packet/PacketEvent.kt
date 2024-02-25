package de.infinityprojects.mpm.packet

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player

class PacketEvent<T : Packet<*>>(var packet: T, val player: Player) {
    var cancelled: Boolean = false
}