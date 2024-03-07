package de.infinityprojects.mpm.item.mutable

import de.infinityprojects.mpm.block.handler.BlockProvider
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.item.handler.ItemProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class MPMItem(val itemStack: ItemStack, val item: Item) : MutableItem {
    private val itemMeta
        get() = itemStack.itemMeta

    private val tags = itemMeta.persistentDataContainer

    fun <P, C> getTag(
        key: String,
        type: PersistentDataType<P, C>,
    ): C? {
        val nk = getKey(key)
        return tags.get(nk, PersistentDataType.STRING) as? C
    }

    fun <P, C> setTag(
        key: String,
        type: PersistentDataType<P, C>,
        value: C & Any,
    ) {
        val nk = getKey(key)
        tags.set(nk, type, value)
    }

    fun getKey(key: String): NamespacedKey {
        return NamespacedKey(item.getPluginName(), key)
    }

    override fun getAmount(): Int {
        return itemStack.amount
    }

    override fun setAmount(amount: Int) {
        val max = getTag("max-stack", PersistentDataType.INTEGER) ?: 64
        itemStack.amount = amount.coerceIn(1, max)
    }

    override fun getDisplayName(): Component {
        val name = getTag("display-name", PersistentDataType.STRING)
        val component =
            if (name != null) {
                LegacyComponentSerializer.legacySection().deserialize(name)
            } else {
                when (item) {
                    is BlockProvider -> item.displayName
                    is ItemProvider -> item.displayName // TODO: Declare displayName in Item
                    else ->
                        Component.text("⚠ ERROR ⚠").style(
                            Style.style().color(
                                NamedTextColor.DARK_RED,
                            ).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                        )
                }
            }
        return component
    }

    override fun setDisplayName(name: Component) {
        val legacy = LegacyComponentSerializer.legacySection().serialize(name)
        setTag("display-name", PersistentDataType.STRING, legacy)
    }

    override fun getDescription(): List<Component> {
        val lore = getTag("lore", PersistentDataType.STRING)
        val component =
            if (lore != null) {
                lore.split("\n").map { LegacyComponentSerializer.legacySection().deserialize(it) }
            } else {
                when (item) {
                    is BlockProvider -> item.desc
                    is ItemProvider -> item.desc
                    else ->
                        listOf(
                            Component.text("⚠ ERROR ⚠").style(
                                Style.style().color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD),
                            ),
                        )
                }
            }
        return component
    }

    override fun setDescription(desc: List<Component>) {
        val legacy = desc.joinToString("\n") { LegacyComponentSerializer.legacySection().serialize(it) }
        setTag("lore", PersistentDataType.STRING, legacy)
    }

    fun getEnchantmentContainer(): PersistentDataContainer {
        val container = getTag("enchantments", PersistentDataType.TAG_CONTAINER)
        if (container == null) {
            val newContainer = tags.adapterContext.newPersistentDataContainer()
            setTag("enchantments", PersistentDataType.TAG_CONTAINER, newContainer)
            return newContainer
        }
        return container
    }

    override fun getEnchantments(): Map<Enchantment, Int> {
        val enchantments = getEnchantmentContainer()
        val reg = Registry.ENCHANTMENT
        val ench = enchantments.keys.mapNotNull {
            val enchantment = reg.get(it) ?: return@mapNotNull null
            val level = enchantments.get(it, PersistentDataType.INTEGER) ?: return@mapNotNull null
            enchantment to level
        }.toMap()
        return ench
    }

    override fun setEnchantments(enchantments: Map<Enchantment, Int>) {
        val container = getEnchantmentContainer()
        container.keys.forEach { container.remove(it) }
        val reg = Registry.ENCHANTMENT
        enchantments.forEach { (e, l) ->
            reg.getKey(e)?.let { container.set(it, PersistentDataType.INTEGER, l) }
        }
    }

    override fun addEnchantment(
        enchantment: Enchantment,
        level: Int,
    ) {
        val container = getEnchantmentContainer()
        val reg = Registry.ENCHANTMENT
        reg.getKey(enchantment)?.let { container.set(it, PersistentDataType.INTEGER, level) }
    }

    override fun removeEnchantment(enchantment: Enchantment) {
        val container = getEnchantmentContainer()
        val reg = Registry.ENCHANTMENT
        reg.getKey(enchantment)?.let { container.remove(it) }
    }

    override fun hasEnchantment(enchantment: Enchantment): Boolean {
        val container = getEnchantmentContainer()
        val reg = Registry.ENCHANTMENT
        return reg.getKey(enchantment) in container.keys
    }

    override fun removeEnchantments() {
        val container = getEnchantmentContainer()
        container.keys.forEach { container.remove(it) }
    }

    override fun getEnchantmentLevel(enchantment: Enchantment): Int {
        val container = getEnchantmentContainer()
        val reg = Registry.ENCHANTMENT
        return reg.getKey(enchantment)?.let { container.get(it, PersistentDataType.INTEGER) } ?: 0
    }

    override fun doUseDamage() {
        if (canDoUseDamage()) {
            println("Not yet implemented")
        }
    }

    @Deprecated("Probably no use")
    fun reloadItem(player: Player) {
        itemStack.itemMeta = itemMeta
        player.updateInventory()
    }
}
