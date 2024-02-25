package de.infinityprojects.mpm.providers

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.item.data.ItemData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.Yaml

class ItemProvider private constructor(
    val id: NamespacedKey,
    val data: ItemData?,
    val displayName: Component,
    val desc: List<Component>,
    var model: Int = 0,
): Item {
    override fun getItem(): ItemStack {
        val item = ItemStack(Material.STONE)
        val meta = item.itemMeta ?: Bukkit.getItemFactory().getItemMeta(Material.STONE)
        val name = Component.text("⚠ ERROR ⚠").style(
            Style.style().decorate(TextDecoration.BOLD).color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        )

        meta.displayName(name)

        val loreStyle = Style.style().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).build()
        val lore = listOf(
            "You should not see this item",
            "",
            "Possible issues:",
            " - There's a bug in MPM",
            " - Item was removed",
            " - Mod was removed",
            " - Mod cannot load",
            " - MPM cannot load properly",
            "",
            "* If you're a user, try rejoining ",
            "the server and contact an",
            "administrator.",
            "",
            "$id"
        ).map { Component.text(it).style(loreStyle) }
        meta.lore(
           lore
        )
        meta.persistentDataContainer.set(id, PersistentDataType.STRING, "mpm")
        //meta.setCustomModelData(model)
        item.itemMeta = meta
        return item
    }

    override fun getItem(amount: Int): ItemStack {
        val item = getItem()
        item.amount = amount
        return item
    }

    companion object {
        private val yaml: Yaml = Yaml()
        private var tempTranslations: Map<String, String> = mapOf()

        fun createItem(plugin: Plugin, id: String, data: ItemData?): Item {
            val model = (Manager.getManager().getItemRegistry() as RegistryProvider).dispatchID()

            if (tempTranslations.isEmpty()) {
                val translationIS = plugin.getResource("translations.yml")
                if (translationIS != null) {
                    val translations: Map<String, String> = yaml.load(translationIS)
                    tempTranslations = translations
                } else {
                    tempTranslations = mapOf("null" to "null")
                }
            }

            val displayName = Component.text(tempTranslations[id] ?: id).style(
                Style.style().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            )


            return ItemProvider(NamespacedKey(plugin, id), data, displayName, listOf(), model)
        }
    }
}