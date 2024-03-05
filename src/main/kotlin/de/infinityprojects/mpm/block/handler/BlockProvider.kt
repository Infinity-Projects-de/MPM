package de.infinityprojects.mpm.block.handler

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.Block
import de.infinityprojects.mpm.block.BlockData
import de.infinityprojects.mpm.block.BlockModelData
import de.infinityprojects.mpm.item.data.ItemData
import de.infinityprojects.mpm.providers.RegistryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.Yaml

class BlockProvider private constructor(
    val id: NamespacedKey,
    val data: BlockData,
    val displayName: Component,
    val desc: List<Component>,
    val modelID: Int,
    val multisided: Boolean,
    val model: BlockModelData = BlockModelData(modelID - BLOCK_MODEL_OFFSET),
) : Block {
    val plugin = id.namespace()

    override fun setBlock(location: Location) {
        val block = location.block
        block.type = Material.BEDROCK
    }

    override fun getBlockData(): BlockData {
        return data
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
                "$id",
            ).map { Component.text(it).style(loreStyle) }
        meta.lore(
            lore,
        )
        meta.persistentDataContainer.set(id, PersistentDataType.STRING, "mpm-block")

        item.itemMeta = meta
        return item
    }

    override fun getItem(amount: Int): ItemStack {
        val item = getItem()
        item.amount = amount
        return item
    }

    override fun getItemData(): ItemData {
        return ItemData.simple()
    }

    override fun getPluginName(): String {
        return plugin
    }

    fun getModelObject(): JsonObject {
        val modelJson = JsonObject()
        modelJson.addProperty("parent", "block/cube")

        val textures = JsonObject()
        textures.addProperty("all", "$plugin:block/${id.key}")
        textures.addProperty("particle", "$plugin:block/${id.key}")
        modelJson.add("textures", textures)
        val elements = JsonObject()
        val from = JsonArray()
        val to = JsonArray()
        for (i in 0..2) {
            from.add(0)
            to.add(16)
        }
        elements.add("from", from)
        elements.add("to", to)

        val faces = JsonObject()
        if (multisided) {
            val textureSize = JsonArray()
            textureSize.add(64)
            textureSize.add(64)
            modelJson.add("texture_size", textureSize)

            for ((name, json) in SIDES) {
                faces.add(name, json)
            }
        } else {
            for (face in SIDES.keys) {
                faces.add(face, SIDE)
            }
        }

        elements.add("faces", faces)

        modelJson.add("elements", JsonArray().apply { add(elements) })

        return modelJson
    }

    val texture = "$plugin:block/${id.key}"

    fun getOverrideObject(): JsonObject {
        val override = JsonObject()
        val predicate = JsonObject()
        predicate.addProperty("custom_model_data", modelID)
        override.add("predicate", predicate)
        override.addProperty("model", texture)
        return override
    }

    fun getVariant(): JsonObject {
        val model = JsonObject()
        model.addProperty("model", texture)
        return model
    }

    companion object {
        const val BLOCK_MODEL_OFFSET = 999
        val MATERIAL = Material.STICK

        private val SIDES =
            mapOf(
                "north" to arrayOf(0, 0, 4, 4),
                "east" to arrayOf(0, 4, 4, 8),
                "south" to arrayOf(4, 0, 8, 4),
                "west" to arrayOf(4, 4, 8, 8),
                "up" to arrayOf(4, 12, 0, 8),
                "down" to arrayOf(12, 0, 8, 4),
            ).map {
                val face = JsonObject()
                val uv = JsonArray()
                it.value.forEach { i ->
                    uv.add(i)
                }

                face.add("uv", uv)
                face.addProperty("texture", "#all")
                it.key to face
            }.toMap() // TODO: Only used on initialization, might be wise to destroy after

        private val SIDE =
            JsonObject().apply {
                val uv =
                    JsonArray().apply {
                        add(0)
                        add(0)
                        add(16)
                        add(16)
                    }
                add("uv", uv)
                addProperty("texture", "#all")
            }

        private val yaml: Yaml = Yaml()
        private var tempTranslations: Map<String, String> = mapOf()

        internal fun createBlock(
            plugin: Plugin,
            id: String,
            data: BlockData?,
            multisided: Boolean = false,
        ): Block {
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

            val model = (Manager.getManager().getBlockRegistry() as RegistryProvider).dispatchID()

            return BlockProvider(NamespacedKey(plugin, id), data ?: BlockData(), displayName, listOf(), model, multisided)
        }
    }
}
