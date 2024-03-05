package de.infinityprojects.mpm.utils

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.providers.RegistryProvider
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class GiveCommand() : TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?,
    ): MutableList<String> {
        val manager = Manager.getManager()

        if (args == null) return mutableListOf()

        if (args.size == 1) {
            return mutableListOf("item", "block")
        }

        if (args.isEmpty()) {
            return mutableListOf()
        }

        val registry =
            (
                if (args[0] == "item") {
                    manager.getItemRegistry()
                } else {
                    manager.getBlockRegistry()
                }
            ) as RegistryProvider

        if (args.size == 2) {
            val plugins = registry.getAll().keys.map { it.namespace() }.toSet()
            return plugins.toMutableList()
        }

        if (args.size == 3) {
            val plugin = Bukkit.getPluginManager().getPlugin(args[1]) ?: return mutableListOf()
            val itemsFilter = registry.getAll(plugin).keys.map { it.key }.toSet()
            return itemsFilter.toMutableList()
        }

        return mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?,
    ): Boolean {
        if (args?.size == 3) {
            val player = sender as? Player ?: return false

            val manager = Manager.getManager()
            val registry =
                (
                    if (args[0] == "item") {
                        manager.getItemRegistry()
                    } else {
                        manager.getBlockRegistry()
                    }
                ) as RegistryProvider

            val plugin = Bukkit.getPluginManager().getPlugin(args[1]) ?: return false
            val item = registry.get(plugin, args[2])
            player.inventory.addItem(item.getItem())
            return true
        }
        return false
    }
}
