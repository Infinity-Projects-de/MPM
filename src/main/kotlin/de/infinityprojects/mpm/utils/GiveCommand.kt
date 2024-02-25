package de.infinityprojects.mpm.utils

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.providers.RegistryProvider
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class GiveCommand(): TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>? {
        val manager = Manager.getManager()
        val items = manager.getItemRegistry() as RegistryProvider<Item>
        if (args?.size == 1) {
            val plugins = items.getAll().keys.map { it.namespace() }.toSet()
            return plugins.toMutableList()
        }

        if (args?.size == 2) {
            val plugin = Bukkit.getPluginManager().getPlugin(args[0]) ?: return mutableListOf()
            val itemsFilter = items.getAll(plugin).keys.map { it.key }.toSet()
            return itemsFilter.toMutableList()
        }

        return mutableListOf()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (args?.size == 2) {
            val player = sender as? Player ?: return false

            val manager = Manager.getManager()
            val items = manager.getItemRegistry() as RegistryProvider<Item>
            val plugin = Bukkit.getPluginManager().getPlugin(args[0]) ?: return false
            val item = items.get(plugin, args[1])
            player.inventory.addItem(item.getItem())
            return true
        }
        return false
    }
}