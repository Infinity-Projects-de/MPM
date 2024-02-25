package de.infinityprojects.mpm.api

import de.infinityprojects.mpm.providers.RegistryProvider
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

interface Registry<T> {
    val type: Class<T>
    fun register(plugin: Plugin, name: String, t: T)
    fun get(key: NamespacedKey): T
    fun get(plugin: Plugin, name: String): T
    fun getAll(plugin: Plugin): Map<NamespacedKey, T>

    fun isType(t: Class<*>): Boolean {
        return type == t
    }

    companion object {
        // may be better in RegistryProvider and not here
        fun <T> createRegistry(t: Class<T>): Registry<T> {
            return RegistryProvider(t)
        }
    }
}