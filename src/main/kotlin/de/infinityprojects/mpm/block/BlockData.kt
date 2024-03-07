package de.infinityprojects.mpm.block

import de.infinityprojects.mpm.item.data.ToolData
import de.infinityprojects.mpm.item.data.ToolType
import de.infinityprojects.mpm.item.mutable.BukkitItem
import de.infinityprojects.mpm.item.mutable.MPMItem
import de.infinityprojects.mpm.item.mutable.MutableItem
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import kotlin.math.min
import kotlin.math.pow

class BlockData(
    val hardness: Float = 1.0f,
    val toolType: List<ToolType> = listOf(),
    val toolLevel: Int = 0,
) {
    constructor(hardness: Float, toolType: ToolType, toolLevel: Int) : this(hardness, listOf(toolType), toolLevel)

    fun canBreak(
        toolType: ToolType,
        toolLevel: Int,
    ): Boolean {
        return this.toolType.contains(toolType) && this.toolLevel <= toolLevel
    }

    fun isToolTypeCorrect(item: ItemStack): Boolean {
        val type =
            when (item.type.name.split("_").last()) {
                "PICKAXE" -> ToolType.PICKAXE
                "AXE" -> ToolType.AXE
                "SHOVEL" -> ToolType.SHOVEL
                "HOE" -> ToolType.HOE
                "SWORD" -> ToolType.SWORD
                else -> ToolType.NONE
            }
        return isToolTypeCorrect(type)
    }

    fun isToolTypeCorrect(type: ToolType): Boolean {
        if (!isToolRequired()) return true
        return toolType.contains(type)
    }

    fun isLevelCorrect(item: ItemStack): Boolean {
        val level =
            when (item.type.name.split("_").first()) {
                "WOODEN" -> 1
                "STONE" -> 2
                "IRON" -> 3
                "GOLDEN" -> 3
                "DIAMOND" -> 5
                else -> 0
            }

        return isLevelCorrect(level)
    }

    fun isLevelCorrect(level: Int): Boolean {
        return level >= toolLevel
    }

    fun isToolRequired(): Boolean {
        return toolType.isNotEmpty()
    }

    fun breakSpeed(
        item: ItemStack,
        player: Player,
    ): Float {
        val mutableItem = if (!item.type.isAir) MutableItem.fromItemStack(item) else null

        // BEST TOOL: Tool affects block but tier is not necessarily enough
        val bestTool: Boolean

        // CAN HARVEST: Tool and tier are correct for breaking the block
        val canHarvest: Boolean

        var speedMultiplier = 1f

        when (mutableItem) {
            is BukkitItem -> {
                canHarvest = (isToolTypeCorrect(item) && isLevelCorrect(item)) || !isToolRequired()
                bestTool = isToolTypeCorrect(item)
                if (bestTool) speedMultiplier = 1.0f // itemStack.tierDestroyDamage()
            }

            is MPMItem -> {
                val mItem = mutableItem.item
                val data = mItem.getItemData()
                if (data is ToolData) {
                    canHarvest = (isToolTypeCorrect(data.toolType) && isLevelCorrect(data.miningLevel)) || !isToolRequired()
                    bestTool = isToolTypeCorrect(data.toolType)
                    if (bestTool) speedMultiplier = data.miningSpeed
                } else {
                    canHarvest = !isToolRequired()
                    bestTool = false
                }
            }

            else -> {
                canHarvest = !isToolRequired()
                bestTool = false
            }
        }

        if (!canHarvest) speedMultiplier = 1f

        // + EFFICIENCY (IF BEST TOOL & EFFICIENCY)
        val efficiencyLevel = item.getEnchantmentLevel(Enchantment.DIG_SPEED)
        if (bestTool && efficiencyLevel > 0) speedMultiplier += (1 + efficiencyLevel * efficiencyLevel)

        // * HASTE (IF ANY)
        val hasteLevel = player.getPotionEffect(PotionEffectType.FAST_DIGGING)?.amplifier ?: 0
        if (hasteLevel > 0) speedMultiplier *= 0.2f * hasteLevel + 1

        // * FATIGUE (IF ANY)
        val fatigueLevel = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)?.amplifier ?: 0
        if (fatigueLevel > 0) speedMultiplier *= 0.3.pow(min(fatigueLevel, 4)).toFloat()

        // PLAYER IN WATER -> /5
        if (player.isInWater) {
            val helmet = player.inventory.helmet
            if ((helmet?.getEnchantmentLevel(Enchantment.WATER_WORKER) ?: 0) < 1) speedMultiplier /= 5
        }

        // PLAYER NOT ON GROUND -> /5
        if (!player.isOnGround) speedMultiplier /= 5 // IsOnGround is a client-side check... so this is not reliable

        return speedMultiplier / (if (canHarvest) 30 else 100)
    }
}
