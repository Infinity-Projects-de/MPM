package de.infinityprojects.mpm

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.Block
import de.infinityprojects.mpm.block.handler.BlockHandler
import de.infinityprojects.mpm.block.handler.BlockProvider
import de.infinityprojects.mpm.block.handler.BlockRenderer
import de.infinityprojects.mpm.block.handler.BreakHandler
import de.infinityprojects.mpm.block.handler.NoteblockDisabler
import de.infinityprojects.mpm.block.handler.PlaceListener
import de.infinityprojects.mpm.block.storage.Region
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.item.handler.ItemHandler
import de.infinityprojects.mpm.item.handler.ItemProvider
import de.infinityprojects.mpm.packet.PacketHandler
import de.infinityprojects.mpm.providers.ManagerService
import de.infinityprojects.mpm.providers.RegistryProvider
import de.infinityprojects.mpm.textures.PlayerJoinResourcePack
import de.infinityprojects.mpm.textures.ResourcePackServer
import de.infinityprojects.mpm.textures.ZipCreator
import de.infinityprojects.mpm.utils.GiveCommand
import de.infinityprojects.mpm.utils.Registers
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class Main : JavaPlugin() {
    override fun onEnable() {
        val manager = ManagerService()
        manager.declareRegistry(Registers.ITEM, RegistryProvider(Item::class.java, 1000))
        manager.declareRegistry(
            Registers.BLOCK,
            RegistryProvider(Block::class.java, BlockProvider.BLOCK_MODEL_OFFSET, BlockProvider.BLOCK_MODEL_OFFSET + 25 * 23 * 2),
        ) // TODO Check if this is the right maxID

        if (ItemProvider.MATERIAL.isBlock || BlockProvider.MATERIAL.isBlock) {
            throw IllegalArgumentException("Material cannot be a block for providers")
        }
        if (ItemProvider.MATERIAL == BlockProvider.MATERIAL) {
            throw IllegalArgumentException("Material cannot be the same for providers")
        }

        server.servicesManager.register(Manager::class.java, manager, this, ServicePriority.Normal)

        val command = getCommand("mpm")
        val c = GiveCommand()
        command?.setExecutor(c)
        command?.tabCompleter = c

        registerListener(PacketHandler)
        PacketHandler.registerListeners(ItemHandler(this))
        PacketHandler.registerListeners(BlockHandler(this))
        registerListener(PlaceListener())
        registerListener(BlockRenderer())
        registerListener(NoteblockDisabler())

        // Test methods not MPM
        Item.registerItem(this, "aether_stick", null)
        Block.registerBlock(this, "aether_dirt", null, false)
        Block.registerBlock(this, "aether_grass_block", null, true)
        // END Test methods

        BreakHandler(this)

        Bukkit.getScheduler().runTaskLater(
            this,
            Runnable {
                createTextures(manager)
            },
            1L,
        )

        Bukkit.getAsyncScheduler().runAtFixedRate(this, {
            Region.saveAll()
        }, 5, 5, TimeUnit.MINUTES)
    }

    override fun onDisable() {
        Region.saveAll()
    }

    val plugins = mutableMapOf<String, Plugin>()

    private fun getPlugin(name: String): Plugin {
        if (!plugins.contains(name)) {
            plugins[name] = server.pluginManager.getPlugin(name)
                ?: throw IllegalArgumentException("Plugin $name does not exist")
        }
        return plugins[name]!!
    }

    private fun createBlocks(
        manager: Manager,
        zip: ZipCreator,
    ) {
        val blockRegistry = manager.getBlockRegistry() as RegistryProvider<Block>
        val overrides = JsonArray()
        val variants = JsonObject()

        blockRegistry.getAll().map { it.value as BlockProvider }.forEach {
            val pluginName = it.getPluginName()
            val plugin = getPlugin(pluginName)
            val blockName = it.id.key

            val file =
                plugin.getResource("textures/block/$blockName.png")
                    ?: throw IllegalArgumentException("Texture for $blockName does not exist")

            zip.writeStream(file, "assets/$pluginName/textures/block/$blockName.png")

            zip.writeData(it.getModelObject(), "assets/$pluginName/models/block/$blockName.json")
            overrides.add(it.getOverrideObject())

            variants.add(it.model.variantName, it.getVariant())
        }

        val itemMaterial = BlockProvider.MATERIAL.name.lowercase()

        val overrideJson = JsonObject()
        overrideJson.addProperty("parent", "minecraft:item/handheld")

        val textures = JsonObject()
        textures.addProperty("layer0", "minecraft:item/$itemMaterial")
        overrideJson.add("textures", textures)

        overrideJson.add("overrides", overrides)

        zip.writeData(overrideJson, "assets/minecraft/models/item/$itemMaterial.json")

        val variantJson = JsonObject()
        variantJson.add("variants", variants)

        zip.writeData(variantJson, "assets/minecraft/blockstates/note_block.json")
    }

    private fun createItems(
        manager: Manager,
        zip: ZipCreator,
    ) {
        val itemRegistry = manager.getItemRegistry() as RegistryProvider<Item>
        val overrides = JsonArray()

        itemRegistry.getAll().map { it.value as ItemProvider }.forEach {
            val pluginName = it.getPluginName()
            val plugin = getPlugin(pluginName)
            val itemName = it.id.key

            val file =
                plugin.getResource("textures/item/$itemName.png")
                    ?: throw IllegalArgumentException("Texture for $itemName does not exist")

            zip.writeStream(file, "assets/$pluginName/textures/item/$itemName.png")

            zip.writeData(it.getModelObject(), "assets/$pluginName/models/item/$itemName.json")
            overrides.add(it.getOverrideObject())
        }

        val itemMaterial = ItemProvider.MATERIAL.name.lowercase()

        val overrideJson = JsonObject()
        overrideJson.addProperty("parent", "minecraft:item/handheld")

        val textures = JsonObject()
        textures.addProperty("layer0", "minecraft:item/$itemMaterial")
        overrideJson.add("textures", textures)

        overrideJson.add("overrides", overrides)

        zip.writeData(overrideJson, "assets/minecraft/models/item/$itemMaterial.json")
    }

    fun createTextures(manager: Manager) {
        val creator = ZipCreator(this)
        val files: MutableMap<String, YamlConfiguration> = mutableMapOf()
        val plugins: MutableMap<String, Plugin> = mutableMapOf()

        createItems(manager, creator)
        createBlocks(manager, creator)

        val rootCopy = listOf("pack.mcmeta", "pack.png")
        rootCopy.forEach {
            val file =
                this.getResource(it)
                    ?: throw IllegalArgumentException("$it does not exist")
            creator.writeStream(file, it)
        }

        creator.close()

        ResourcePackServer(creator.getBytes())
        registerListener(PlayerJoinResourcePack(creator.getSha1()))
    }

    fun registerListener(listener: Listener) {
        server.pluginManager.registerEvents(listener, this)
    }
}
