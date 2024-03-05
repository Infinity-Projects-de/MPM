package de.infinityprojects.mpm.block

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import org.bukkit.Bukkit
import org.bukkit.Instrument
import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.block.data.BlockData

class BlockModelData(model: Int) {
    @Deprecated("Block state doesn't match Bukkit data anymore")
    val blockState: BlockState
    val blockData: BlockData
    private val powered: Int
    private val note: Int
    private val instrument: Int

    init {
        powered = model % 2
        val m = model shr 1
        note = m % 25 // model / 2
        instrument = m / 25

        val block = Blocks.NOTE_BLOCK
        val data = block.defaultBlockState()
        data.setValue(NoteBlock.NOTE, note)
        data.setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.entries[instrument])
        data.setValue(NoteBlock.POWERED, powered == 1)
        blockState = data // deprecate as it doesn't match bukkit data

        val bData = Bukkit.createBlockData(Material.NOTE_BLOCK) as org.bukkit.block.data.type.NoteBlock
        bData.instrument = Instrument.entries[instrument]
        bData.note = Note(note)
        bData.isPowered = powered == 1
        blockData = bData
    }

    val variantName: String
        get() {
            val instrument = Instrument.entries[instrument]
            val sound = instrument.sound ?: throw IllegalStateException("Instrument has no sound")
            val soundName = sound.key.key
            val instrumentName = soundName.split('.').last()
            return "instrument=$instrumentName,note=$note,powered=${powered == 1}"
        }
}
