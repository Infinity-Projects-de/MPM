package de.infinityprojects.mpm.block.storage

import de.infinityprojects.mpm.api.Manager
import de.infinityprojects.mpm.block.Block
import de.infinityprojects.mpm.block.handler.BlockProvider
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import java.io.File
import kotlin.math.abs

// TODO: Unefficent, should be changed to a more efficent storage system
class Region(
    val world: World,
    val x: Int,
    val z: Int,
    val file: File,
) {
    val blocks: MutableMap<Position, Block> = mutableMapOf()

    init {
        val data = file.readText()
        val lines = data.split("\n")
        for (line in lines) {
            if (line.isEmpty()) continue
            val parts = line.split(" ")
            val x = parts[0].toInt()
            val y = parts[1].toInt()
            val z = parts[2].toInt()
            val id = parts[3]
            val idSplit = id.split(":")
            val plugin = idSplit[0]
            val key = idSplit[1]
            val namespacedKey = NamespacedKey(plugin, key)

            val block = Manager.getManager().getBlockRegistry().get(namespacedKey)
            blocks[Position(x, y, z)] = block
        }
    }

    fun save() {
        val data =
            blocks.map { (pos, block) ->
                val id = (block as BlockProvider).id.toString()
                val x = pos.x
                val y = pos.y
                val z = pos.z
                "$x $y $z $id"
            }.joinToString("\n")
        file.writeText(data)
    }

    fun getBlock(
        x: Int,
        y: Int,
        z: Int,
    ): Block? {
        return blocks[Position(x, y, z)]
    }

    fun setBlock(
        x: Int,
        y: Int,
        z: Int,
        block: Block,
    ) {
        blocks[Position(x, y, z)] = block
    }

    fun removeBlock(
        x: Int,
        y: Int,
        z: Int,
    ) {
        blocks.remove(Position(x, y, z))
    }

    companion object {
        val regions = mutableMapOf<Pair<Int, Int>, Region>()
        val folder = Bukkit.getPluginManager().getPlugin("mpm")!!.dataFolder

        fun getRegion(
            world: World,
            x: Int,
            z: Int,
        ): Region {
            val reg = regions[Pair(x, z)]
            if (reg != null) return reg
            val regFolder = folder.resolve(world.name.lowercase()).resolve("regions")
            if (!regFolder.exists()) regFolder.mkdirs()
            val region = regFolder.resolve("$x-$z.mpmr")
            if (!region.exists()) region.createNewFile()

            val reg2 = Region(world, x, z, region)
            regions[Pair(x, z)] = reg2

            return reg2
        }

        fun getBlockAt(
            world: World,
            position: Position,
        ): Block? {
            val region = getRegion(world, position.x / 16, position.z / 16)
            return region.getBlock(abs(position.x % 16), position.y, abs(position.z % 16))
        }

        @Deprecated("Use getBlockAt(World, Position) instead? Not sure")
        fun getBlockAt(
            world: World,
            x: Int,
            y: Int,
            z: Int,
        ): Block? {
            val region = getRegion(world, x / 16, z / 16)
            return region.getBlock(abs(x % 16), y, abs(z % 16))
        }

        fun setBlockAt(
            world: World,
            position: Position,
            block: Block?,
        ) {
            val region = getRegion(world, position.x / 16, position.z / 16)
            if (block == null) {
                region.removeBlock(abs(position.x % 16), position.y, abs(position.z % 16))
            } else {
                region.setBlock(abs(position.x % 16), position.y, abs(position.z % 16), block)
            }
        }

        fun setBlockAt(
            world: World,
            x: Int,
            y: Int,
            z: Int,
            block: Block?,
        ) {
            val position = Position(x, y, z)
            setBlockAt(world, position, null)
        }

        fun saveAll() {
            for (region in regions.values) {
                region.save()
            }
        }
    }
}
