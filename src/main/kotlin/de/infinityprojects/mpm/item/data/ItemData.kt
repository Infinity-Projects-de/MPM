package de.infinityprojects.mpm.item.data

abstract class ItemData(val type: ItemType = ItemType.ITEM) {
    companion object {
        fun simple(): ItemData {
            return object : ItemData() {}
        }
    }
}
