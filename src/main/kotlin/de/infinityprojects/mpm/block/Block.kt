package de.infinityprojects.mpm.block

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.handler.BlockProvider
import de.infinityprojects.mpm.item.Item
import org.bukkit.Location
import org.bukkit.plugin.Plugin

interface Block : Item {
    fun setBlock(location: Location)

    fun getBlockData(): BlockData

    companion object {
        fun registerBlock(
            plugin: Plugin,
            id: String,
            data: BlockData? = null,
            multisided: Boolean = false,
        ) {
            val block = BlockProvider.createBlock(plugin, id, data, multisided)
            Manager.getManager().getBlockRegistry().register(plugin, id, block)
        }

        fun registerItems(
            plugin: Plugin,
            items: Map<String, BlockData?>,
        ) {
            items.forEach { (id, data) ->
                registerBlock(plugin, id, data, false) // TODO: Add multisided possibility to the map
            }
        }
    }
}
