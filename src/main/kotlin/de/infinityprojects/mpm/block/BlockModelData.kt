package de.infinityprojects.mpm.block

import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument

class NoteblockData(model: Int) {
    val blockState: BlockState;
    val powered: Int;
    val note: Int;
    val instrument: Int;

    init {
        powered = model % 2
        val m = model shr 1
        note = m % 25 // model / 2
        instrument = m / 25

        val properties = BlockBehaviour.Properties.of()
        properties.destroyTime(15.0f)

        val noteblock = NoteBlock(properties)

        val data = noteblock.defaultBlockState()
        data.setValue(NoteBlock.NOTE, note)
        data.setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.entries[instrument])
        data.setValue(NoteBlock.POWERED, powered == 1)
        blockState = data
    }
}