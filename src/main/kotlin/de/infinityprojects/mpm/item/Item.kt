package de.infinityprojects.mpm.item

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.data.ItemData
import de.infinityprojects.mpm.providers.ItemProvider
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

interface Item {
    /**
     * Gives a copy of the support item to be given to a player, cannot be modified
     * @return a copy of the support item (amount of 1)
     */
    fun getItem(): ItemStack

    /**
     * Gives a copy of the support item to be given to a player, cannot be modified
     * @param amount the amount of items to be given
     * @return a copy of the support item
     */
    fun getItem(amount: Int): ItemStack

    companion object {
        fun registerItem(plugin: Plugin, id: String, data: ItemData?) {
            val item = ItemProvider.createItem(plugin, id, data)
            Manager.getManager().getItemRegistry().register(plugin, id, item)
        }

        fun registerItems(plugin: Plugin, items: Map<String, ItemData?>) {
            items.forEach { (id, data) ->
                registerItem(plugin, id, data)
            }
        }
    }
}