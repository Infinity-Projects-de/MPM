package de.infinityprojects.mpm.item.handler

import com.google.gson.JsonObject
import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.item.data.ItemData
import de.infinityprojects.mpm.providers.RegistryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.Yaml

class ItemProvider private constructor(
    val id: NamespacedKey,
    val data: ItemData,
    val displayName: Component,
    val desc: List<Component>,
    val model: Int = 0,
) : Item {
    val plugin = id.namespace()

    override fun getPluginName(): String {
        return plugin
    }

    override fun getItem(): ItemStack {
        val item = ItemStack(MATERIAL)
        val meta = item.itemMeta
        val name =
            Component.text("⚠ ERROR ⚠").style(
                Style.style().decorate(TextDecoration.BOLD).color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false),
            )

        meta.displayName(name)

        val loreStyle = Style.style().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).build()
        val lore =
            listOf(
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
                "$id",
            ).map { Component.text(it).style(loreStyle) }
        meta.lore(
            lore,
        )
        meta.persistentDataContainer.set(id, PersistentDataType.STRING, "mpm-item")

        item.itemMeta = meta
        return item
    }

    override fun getItem(amount: Int): ItemStack {
        val item = getItem()
        item.amount = amount
        return item
    }

    override fun getItemData(): ItemData {
        return data
    }

    val texture = "$plugin:item/${id.key}"

    fun getOverrideObject(): JsonObject {
        val override = JsonObject()
        val predicate = JsonObject()
        predicate.addProperty("custom_model_data", model)
        override.add("predicate", predicate)
        override.addProperty("model", texture)
        return override
    }

    fun getModelObject(): JsonObject {
        val modelJson = JsonObject()
        modelJson.addProperty("parent", "item/handheld")
        val textures = JsonObject()
        textures.addProperty("layer0", texture)
        modelJson.add("textures", textures)
        return modelJson
    }

    companion object {
        val MATERIAL = Material.FLINT
        private val yaml: Yaml = Yaml()
        private var tempTranslations: Map<String, String> = mapOf()

        internal fun createItem(
            plugin: Plugin,
            id: String,
            data: ItemData?,
        ): Item {
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

            val displayName =
                Component.text(tempTranslations[id] ?: id).style(
                    Style.style().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                )

            return ItemProvider(NamespacedKey(plugin, id), data ?: ItemData.simple(), displayName, listOf(), model)
        }
    }
}
