package de.infinityprojects.mpm.item

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.packet.PacketEvent
import de.infinityprojects.mpm.packet.PacketListener
import de.infinityprojects.mpm.providers.ItemProvider
import net.minecraft.core.NonNullList
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.ItemStack
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.persistence.PersistentDataType

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
        if (!bukkitItem.hasItemMeta()) return item
        val meta = bukkitItem.itemMeta
        meta.persistentDataContainer.keys.forEach {
            if (meta.persistentDataContainer.get(it, PersistentDataType.STRING) == "mpm") {
                val plugin = Bukkit.getPluginManager().getPlugin(it.namespace()) ?: throw Exception("Plugin not found")
                val reg = Manager.getManager().getItemRegistry()
                val mpmItem = reg.get(it) as ItemProvider
                val mItem = org.bukkit.inventory.ItemStack(Material.STONE)
                val mMeta = mItem.itemMeta
                mMeta.displayName(mpmItem.displayName)
                mMeta.lore(mpmItem.desc)
                mMeta.setCustomModelData(mpmItem.model)

                mItem.itemMeta = mMeta
                mItem.amount = bukkitItem.amount
                return ItemStack.fromBukkitCopy(mItem)
            }
        }
        return item
    }
}