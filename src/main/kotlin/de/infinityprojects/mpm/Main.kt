package de.infinityprojects.mpm

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.api.Registry
import de.infinityprojects.mpm.item.Item
import de.infinityprojects.mpm.item.ItemHandler
import de.infinityprojects.mpm.packet.PacketHandler
import de.infinityprojects.mpm.providers.ItemProvider
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

class Main : JavaPlugin() {
    override fun onEnable() {
        val manager = ManagerService()
        manager.declareRegistry(Registers.ITEM, Registry.createRegistry(Item::class.java))

        server.servicesManager.register(Manager::class.java, manager, this, ServicePriority.Normal)

        val command = getCommand("mpm")
        val c = GiveCommand()
        command?.setExecutor(c)
        command?.tabCompleter = c

        server.pluginManager.registerEvents(PacketHandler, this)
        PacketHandler.registerListeners(ItemHandler())

        // Test methods not MPM
        Item.registerItem(this, "portal", null)
        // END Test methods

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            createTextures(manager)
        }, 1L)
    }

    private fun createItems(manager: Manager, zip: ZipCreator) {
        val itemRegistry = manager.getItemRegistry() as RegistryProvider<Item>
        val plugins = mutableMapOf<String, Plugin>()
        val models = mutableMapOf<Int, String>()

        itemRegistry.getAll().forEach {
            val pluginName = it.key.namespace()
            if (!plugins.contains(pluginName)) {
                plugins[pluginName] = server.pluginManager.getPlugin(pluginName)
                    ?: throw IllegalArgumentException("Plugin $pluginName does not exist")
            }
            val plugin = plugins[pluginName]!!

            val itemName = it.key.key
            val item = it.value as ItemProvider

            val file = plugin.getResource("textures/item/$itemName.png")
                ?: throw IllegalArgumentException("Texture for $itemName does not exist")

            zip.writeStream(file, "assets/$pluginName/textures/item/$itemName.png")
            models[item.model] = "$pluginName:item/$itemName"

            val modelJson = JsonObject()
            modelJson.addProperty("parent", "item/handheld")
            val textures = JsonObject()
            textures.addProperty("layer0", "$pluginName:item/$itemName")
            modelJson.add("textures", textures)

            val bArray = modelJson.toString().toByteArray()

            zip.writeData(bArray, "assets/$pluginName/models/item/$itemName.json")
        }

        val modelJson = JsonObject()
        modelJson.addProperty("parent", "minecraft:block/stone")
        val overrides = JsonArray()
        models.forEach {
            val override = JsonObject()
            val predicate = JsonObject()
            predicate.addProperty("custom_model_data", it.key)
            override.add("predicate", predicate)
            override.addProperty("model", it.value)
            overrides.add(override)
        } // todo put in main loop
        modelJson.add("overrides", overrides)

        val bArray = modelJson.toString().toByteArray()
        zip.writeData(bArray, "assets/minecraft/models/item/stone.json")

    }

    fun createTextures(manager: Manager) {
        val creator = ZipCreator(this)
        val files: MutableMap<String, YamlConfiguration> = mutableMapOf()
        val plugins: MutableMap<String, Plugin> = mutableMapOf()

        createItems(manager, creator)

        val packData = this.getResource("pack.mcmeta")
            ?: throw IllegalArgumentException("pack.mcmeta does not exist")
        creator.writeStream(packData, "pack.mcmeta")
        val packImage = this.getResource("pack.png")
            ?: throw IllegalArgumentException("pack.png does not exist")
        creator.writeStream(packImage, "pack.png")

        creator.close()

        ResourcePackServer(creator.getBytes())
        registerListener(PlayerJoinResourcePack(creator.getSha1()))
    }

    fun registerListener(listener: Listener) {
        server.pluginManager.registerEvents(listener, this)
    }
}