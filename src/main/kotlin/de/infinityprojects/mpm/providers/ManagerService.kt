package de.infinityprojects.mpm.providers

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.api.Registry
import de.infinityprojects.mpm.block.Block
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.utils.Registers

class ManagerService : Manager {
    private var registers: MutableMap<Registers, Registry<*>> = mutableMapOf()

    override fun <T> getRegistry(t: Class<T>): Registry<T> {
        val registry = registers.values.find { it.isType(t) } as? Registry<T>
        if (registry == null) {
            throw IllegalArgumentException("Registry does not exist")
        }
        return registry
    }

    override fun getItemRegistry(): Registry<Item> {
        return getRegistry(Item::class.java)
    }

    override fun getBlockRegistry(): Registry<Block> {
        return getRegistry(Block::class.java)
    }

    fun getRegistry(r: Registers): Registry<*> {
        val registry = registers[r] ?: throw IllegalArgumentException("Registry does not exist")
        return registry
    }

    fun declareRegistry(
        r: Registers,
        registry: Registry<*>,
    ) {
        val reg = registers[r]
        if (reg != null) {
            throw IllegalArgumentException("Registry already exists")
        }
        registers[r] = registry
    }

    private fun getRegistries(): Map<Registers, Registry<*>> {
        return registers.toMap()
    }
}
