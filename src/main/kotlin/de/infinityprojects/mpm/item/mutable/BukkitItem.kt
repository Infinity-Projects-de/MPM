package de.infinityprojects.mpm.item.mutable

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

class BukkitItem(private val itemStack: ItemStack) : MutableItem {
    private val itemMeta: ItemMeta
        get() = itemStack.itemMeta ?: Bukkit.getItemFactory().getItemMeta(itemStack.type)

    override fun getAmount(): Int {
        return itemStack.amount
    }

    override fun setAmount(amount: Int) {
        if (amount <= itemStack.maxStackSize) {
            itemStack.amount = amount
        } else {
            itemStack.amount = itemStack.maxStackSize
        }
    }

    override fun getDisplayName(): Component {
        return itemMeta.displayName() ?: Component.empty()
    }

    override fun setDisplayName(name: Component) {
        itemMeta.displayName(name)
        applyMeta()
    }

    override fun getDescription(): List<Component> {
        return itemMeta.lore() ?: mutableListOf()
    }

    override fun setDescription(desc: List<Component>) {
        itemMeta.lore(desc)
        applyMeta()
    }

    override fun getEnchantments(): Map<Enchantment, Int> {
        return itemStack.enchantments
    }

    override fun setEnchantments(enchantments: Map<Enchantment, Int>) {
        itemStack.removeEnchantments()
        itemStack.addEnchantments(enchantments)
    }

    override fun addEnchantment(
        enchantment: Enchantment,
        level: Int,
    ) {
        itemStack.addEnchantment(enchantment, level)
    }

    override fun removeEnchantment(enchantment: Enchantment) {
        itemStack.removeEnchantment(enchantment)
    }

    override fun hasEnchantment(enchantment: Enchantment): Boolean {
        return itemStack.containsEnchantment(enchantment)
    }

    override fun removeEnchantments() {
        itemStack.removeEnchantments()
    }

    override fun getEnchantmentLevel(enchantment: Enchantment): Int {
        return itemStack.getEnchantmentLevel(enchantment)
    }

    override fun doUseDamage() {
        if (canDoUseDamage()) {
            val meta = itemMeta
            if (meta is Damageable) {
                meta.damage += 1
                itemStack.itemMeta = meta
            }
        }
    }

    fun applyMeta() {
        itemStack.itemMeta = itemMeta
    }
}
