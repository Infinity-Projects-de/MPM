package de.infinityprojects.mpm.item.mutable

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.Item
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random

interface MutableItem {
    fun getAmount(): Int

    fun setAmount(amount: Int)

    fun getDisplayName(): Component

    fun setDisplayName(name: Component)

    fun getDescription(): List<Component>

    fun setDescription(desc: List<Component>)

    fun getEnchantments(): Map<Enchantment, Int>

    fun setEnchantments(enchantments: Map<Enchantment, Int>)

    fun addEnchantment(
        enchantment: Enchantment,
        level: Int,
    )

    fun removeEnchantment(enchantment: Enchantment)

    fun hasEnchantment(enchantment: Enchantment): Boolean

    fun removeEnchantments()

    fun getEnchantmentLevel(enchantment: Enchantment): Int

    fun canDoUseDamage(): Boolean {
        val durabilityLvl = getEnchantmentLevel(Enchantment.DURABILITY)
        return Random.nextInt(0, durabilityLvl + 1) == 0
    }

    fun doUseDamage()

    companion object {
        fun fromItemStack(item: ItemStack): MutableItem {
            if (item.type.isAir) {
                Bukkit.getLogger().warning("Item is air!")
                return BukkitItem(item)
            }

            val meta = item.itemMeta
            val tags = meta.persistentDataContainer
            val predicates =
                tags.keys.mapNotNull {
                    val value = tags.get(it, PersistentDataType.STRING)
                    if (value != null && value.startsWith("mpm")) it to value else null
                }

            if (predicates.isNotEmpty()) {
                val predicate = predicates.first()
                val mpm =
                    when (predicate.second) {
                        "mpm-item" -> Manager.getManager().getItemRegistry().get(predicate.first)
                        "mpm-block" -> Manager.getManager().getBlockRegistry().get(predicate.first)
                        else -> throw IllegalArgumentException("Unknown mpm type: ${predicate.second}")
                    }
                return MPMItem(item, mpm)
            } else {
                return BukkitItem(item)
            }
        }

        fun fromItem(item: Item): MutableItem {
            val stack = item.getItem()
            return fromItemStack(stack)
        }
    }
}
