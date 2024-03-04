package de.infinityprojects.mpm.block

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.providers.RegistryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Barrel
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.Yaml

class BlockProvider private constructor (
    val id: NamespacedKey,
    val data: BlockData?,
    val displayName: Component,
    val desc: List<Component>,
    val model: BlockModelData,
): Block {
    val plugin = id.namespace()
    override fun setBlock(location: Location) {
        val block = location.block
        block.type = Material.BARREL
        block as Barrel
        val tags = block.persistentDataContainer
        tags.set(id, PersistentDataType.STRING, "mpm-block")
    }

    override fun getItem(): ItemStack {
        val item = ItemStack(Material.STONE)
        val meta = item.itemMeta ?: Bukkit.getItemFactory().getItemMeta(Material.STONE)
        val name = Component.text("⚠ ERROR ⚠").style(
            Style.style().decorate(TextDecoration.BOLD).color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        )

        meta.displayName(name)

        val loreStyle = Style.style().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).build()
        val lore = listOf(
            "You should not see this block",
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
        meta.persistentDataContainer.set(id, PersistentDataType.STRING, "mpm-block")

        item.itemMeta = meta
        return item
    }

    override fun getItem(amount: Int): ItemStack {
        TODO("Not yet implemented")
    }

    override fun getPluginName(): String {
        TODO("Not yet implemented")
    }

    companion object private val yaml: Yaml = Yaml()
    private var tempTranslations: Map<String, String> = mapOf()

    internal fun createBlock(plugin: Plugin, id: String, data: BlockData?): Item {
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


        return BlockProvider(NamespacedKey(plugin, id), data, displayName, listOf(), BlockModelData(model))
    }
}