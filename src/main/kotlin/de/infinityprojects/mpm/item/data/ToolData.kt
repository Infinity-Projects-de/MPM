package de.infinityprojects.mpm.item.data

class ToolData(
    val toolType: ToolType = ToolType.PICKAXE,
    val durability: Int = 0,
    val miningLevel: Int = 0,
    val miningSpeed: Float = 1.0f,
    val attackDamage: Float = 0.0f,
    val attackSpeed: Float = 0.0f,
) : ItemData(ItemType.TOOL)
