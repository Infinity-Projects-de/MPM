package de.infinityprojects.mpm.api

import de.infinityprojects.mpm.item.Item
import org.bukkit.Bukkit

interface Manager {
    fun <T> getRegistry(t: Class<T>): Registry<T>

    fun getItemRegistry(): Registry<Item>

    companion object {
        private lateinit var manager: Manager

        fun getManager(): Manager {
            if (!::manager.isInitialized) {
                manager = Bukkit.getServicesManager().load(Manager::class.java) ?: throw IllegalArgumentException("Manager is not loaded")
            }
            return manager
        }
    }
}