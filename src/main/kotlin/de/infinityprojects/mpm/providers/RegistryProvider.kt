package de.infinityprojects.mpm.providers

import de.infinityprojects.mpm.api.Registry
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

class RegistryProvider<T>(t: Class<T>): Registry<T> {
    override val type: Class<T> = t
    private val register: MutableMap<NamespacedKey, T> = mutableMapOf()

    override fun register(plugin: Plugin, name: String, t: T) {
        val key = NamespacedKey(plugin, name)
        if (register.containsKey(key)) {
            throw IllegalArgumentException("Registry already contains key")
        }
        register[key] = t
    }

    override fun get(plugin: Plugin, name: String): T {
        val key = NamespacedKey(plugin, name)
        return get(key)
    }

    override fun get(key: NamespacedKey): T {
        val t = register[key]
        if (t != null) {
            return t
        }
        throw IllegalArgumentException("Registry does not contain key")
    }

    override fun getAll(plugin: Plugin): Map<NamespacedKey, T> {
        return register.filter {
            it.key.namespace == plugin.name
        }.toMap()
    }

    fun update(plugin: Plugin, name: String, t: T) {
        val key = NamespacedKey(plugin, name)
        if (!register.containsKey(key)) {
            throw IllegalArgumentException("Registry does not contain key")
        }
        register[key] = t
    }

    private var currentID = 1000
    fun dispatchID(): Int {
        return currentID++
    }


    fun getAll(): Map<NamespacedKey, T> {
        return register.toMap()
    }
}