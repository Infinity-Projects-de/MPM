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
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    override fun getEnchantments(): Map<Enchantment, Int> {
        TODO("Not yet implemented")
    }

    override fun setEnchantments(enchantments: Map<Enchantment, Int>) {
        TODO("Not yet implemented")
    }

    override fun addEnchantment(
        enchantment: Enchantment,
        level: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeEnchantment(enchantment: Enchantment) {
        TODO("Not yet implemented")
    }

    override fun hasEnchantment(enchantment: Enchantment): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeEnchantments() {
        TODO("Not yet implemented")
    }

    fun reloadItem(player: Player) {
        itemStack.itemMeta = itemMeta
        player.updateInventory()
    }
}
