package de.infinityprojects.mpm.item

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.mutable.MPMItem
import de.infinityprojects.mpm.packet.PacketEvent
import de.infinityprojects.mpm.packet.PacketListener
import net.minecraft.core.NonNullList
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.ItemStack
import org.bukkit.GameMode
import org.bukkit.Material

class ItemHandler {
    @PacketListener
    fun containerSetPacket(e: PacketEvent<ClientboundContainerSetContentPacket>) {
        val p = e.packet
        if (e.player.gameMode == GameMode.CREATIVE) return

        val reg = Manager.getManager().getItemRegistry()
        val items = NonNullList.create<ItemStack>()
        var changes = false
        for (item in p.items) {
            val rItem = getRealItem(item)
            if (rItem != item) changes = true
            items.add(rItem)
        }

        val carriedItem = getRealItem(p.carriedItem)
        if (changes || carriedItem != p.carriedItem) {
            e.packet = ClientboundContainerSetContentPacket(p.containerId, p.stateId, items, carriedItem)
        }
    }

    @PacketListener
    fun itemSetPacket(e: PacketEvent<ClientboundContainerSetSlotPacket>) {
        val p = e.packet;
        val item = getRealItem(p.item)
        if (item != p.item) {
            e.packet = ClientboundContainerSetSlotPacket(p.containerId, p.stateId, p.slot, item)
        }
    }

    private fun getRealItem(item: ItemStack): ItemStack {
        val bukkitItem = item.asBukkitMirror()
        val mutableItem = MutableItem.fromItemStack(bukkitItem)
        if (mutableItem is MPMItem) {
            val realItem = org.bukkit.inventory.ItemStack(Material.STONE)
            val meta = realItem.itemMeta
            meta.displayName(mutableItem.getDisplayName())
            meta.lore(mutableItem.getDescription())
            meta.setCustomModelData((mutableItem.item as ItemProvider).model)

            realItem.itemMeta = meta
            realItem.amount = bukkitItem.amount
            return ItemStack.fromBukkitCopy(realItem)
        } else {
            return item
        }
    }
}