package de.infinityprojects.mpm.item.handler

import de.infinityprojects.mpm.Main
import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.handler.BlockProvider
import de.infinityprojects.mpm.item.mutable.MPMItem
import de.infinityprojects.mpm.item.mutable.MutableItem
import de.infinityprojects.mpm.packet.PacketEvent
import de.infinityprojects.mpm.packet.PacketListener
import net.minecraft.core.NonNullList
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.world.item.ItemStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

class ItemHandler(val mpm: Main) {
    @PacketListener
    fun containerSetPacket(e: PacketEvent<ClientboundContainerSetContentPacket>) {
        val p = e.packet

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
        val p = e.packet
        val item = getRealItem(p.item)
        if (item != p.item) {
            e.packet = ClientboundContainerSetSlotPacket(p.containerId, p.stateId, p.slot, item)
        }
    }

    @PacketListener
    fun entityPacket(e: PacketEvent<ClientboundBundlePacket>) {
        val b = e.packet
        b.subPackets().forEach { p ->
            if (p is ClientboundSetEntityDataPacket) {
                handleMetadataPacket(p)
            }
        }
    }

    private fun handleMetadataPacket(p: ClientboundSetEntityDataPacket) {
        p.packedItems.forEachIndexed { i, d ->
            if (d.id == 8) {
                val item = d.value
                if (item is ItemStack) {
                    val rItem = getRealItem(item)
                    if (rItem != item) {
                        val data = DataValue.create(EntityDataAccessor(8, EntityDataSerializers.ITEM_STACK), rItem)
                        p.packedItems[i] = data
                    }
                }
            }
        }
    }

    @PacketListener
    fun entityDataPacket(e: PacketEvent<ClientboundSetEntityDataPacket>) {
        handleMetadataPacket(e.packet)
    }

    private fun getRealItem(item: ItemStack): ItemStack {
        val bukkitItem = item.asBukkitMirror()
        if (bukkitItem.type.isAir) return item

        val mutableItem = MutableItem.fromItemStack(bukkitItem)
        if (mutableItem is MPMItem) {
            val material =
                when (mutableItem.item) {
                    is BlockProvider -> BlockProvider.MATERIAL
                    is ItemProvider -> ItemProvider.MATERIAL
                    else -> Material.BARRIER
                }

            val id =
                when (mutableItem.item) {
                    is BlockProvider -> mutableItem.item.id
                    is ItemProvider -> mutableItem.item.id
                    else -> NamespacedKey(Bukkit.getPluginManager().getPlugin("mpm")!!, "null")
                }

            val type = if (mutableItem.item is BlockProvider) "block" else "item"

            val realItem = org.bukkit.inventory.ItemStack(material)
            val meta = realItem.itemMeta
            meta.persistentDataContainer.set(id, PersistentDataType.STRING, "fake-$type")
            meta.displayName(mutableItem.getDisplayName())
            meta.lore(mutableItem.getDescription())

            if (mutableItem.item is ItemProvider) {
                meta.setCustomModelData(mutableItem.item.model)
            } else if (mutableItem.item is BlockProvider) {
                meta.setCustomModelData(mutableItem.item.modelID)
            }

            realItem.itemMeta = meta
            realItem.amount = bukkitItem.amount
            return ItemStack.fromBukkitCopy(realItem)
        } else {
            val meta = bukkitItem.itemMeta
            val data = meta.persistentDataContainer
            data.keys.forEach {
                val value = data.get(it, PersistentDataType.STRING) ?: ""
                if (value.startsWith("fake")) {
                    Bukkit.getLogger().warning(
                        "Player has copied a fake item, trying to fix. [KNOWN BUG] Player is probably in creative mode.",
                    )
                    val mItem =
                        if (value.endsWith("item")) {
                            Manager.getManager().getItemRegistry().get(it)
                        } else {
                            Manager.getManager().getBlockRegistry().get(it)
                        }
                    if (mItem != null) {
                        val itemStack = mItem.getItem(bukkitItem.amount)
                        bukkitItem.type = itemStack.type
                        bukkitItem.itemMeta = itemStack.itemMeta

                        return getRealItem(item)
                    }
                }
            }

            return item
        }
    }
}
