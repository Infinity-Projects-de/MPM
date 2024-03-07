package de.infinityprojects.mpm.block.handler

import de.infinityprojects.mpm.Main
import de.infinityprojects.mpm.block.Block
import de.infinityprojects.mpm.block.storage.Region
import de.infinityprojects.mpm.item.mutable.MutableItem
import de.infinityprojects.mpm.packet.PacketHandler
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import kotlin.math.floor
import kotlin.math.min

class BreakHandler(val mpm: Main) {
    val blocks: HashMap<Player, Destroying> = hashMapOf()

    init {

        handler = this
        Bukkit.getScheduler().runTaskTimer(
            mpm,
            Runnable {
                blocks.forEach { (player, destroying) ->
                    destroying.apply {
                        damage += toolDamage
                        if (block != null) {
                            if (damage >= block.getBlockData().hardness) {
                                if (!item.type.isAir) {
                                    val mutableItem = MutableItem.fromItemStack(item)
                                    mutableItem.doUseDamage()
                                }
                                val item = block.getItem()
                                location.world.dropItemNaturally(location, item)
                                Region.setBlockAt(
                                    location.world,
                                    location.blockX,
                                    location.blockY,
                                    location.blockZ,
                                    null,
                                )
                                Bukkit.getScheduler().runTask(
                                    mpm,
                                    Runnable {
                                        location.block.type = Material.AIR
                                    },
                                )
                                stopBreaking(player)
                                location.world.playSound(location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
                                // TODO: Particles and proper sound
                            }
                        }

                        val stage = min(floor((damage / block!!.getBlockData().hardness * 9)).toInt(), 9)
                        player.sendBlockDestructionPacket(location, stage)
                    }
                }
            },
            0,
            1L,
        )
    }

    data class Destroying(
        var damage: Float,
        val location: Location,
        val toolDamage: Float,
        val block: Block?,
        val item: ItemStack,
    )

    companion object {
        private val MINING_FATIGUE = MobEffectInstance(MobEffects.DIG_SLOWDOWN, Integer.MAX_VALUE, 255, false, false, false)

        lateinit var handler: BreakHandler

        private fun Player.disableBreakingAnimation() {
            val effect = this.getPotionEffect(PotionEffectType.SLOW_DIGGING)
            val packet =
                if (effect != null) {
                    // the player might actually have mining fatigue.
                    // in this case, it is important to copy the hasIcon value to prevent it from disappearing.
                    val effectInstance =
                        MobEffectInstance(
                            MobEffects.DIG_SLOWDOWN,
                            Int.MAX_VALUE,
                            255,
                            effect.isAmbient,
                            effect.hasParticles(),
                            effect.hasIcon(),
                        )
                    ClientboundUpdateMobEffectPacket(this.entityId, effectInstance)
                } else {
                    // the player does not have mining fatigue, we can use the default effect instance
                    ClientboundUpdateMobEffectPacket(this.entityId, MINING_FATIGUE)
                }

            PacketHandler.sendPacket(this, packet)
        }

        private fun Player.enableBreakingAnimation() {
            val effect = this.getPotionEffect(PotionEffectType.SLOW_DIGGING)
            val packet =
                if (effect != null) {
                    // if the player actually has mining fatigue, send the correct effect again
                    val effectInstance =
                        MobEffectInstance(
                            MobEffects.DIG_SLOWDOWN,
                            effect.duration,
                            effect.amplifier,
                            effect.isAmbient,
                            effect.hasParticles(),
                            effect.hasIcon(),
                        )
                    ClientboundUpdateMobEffectPacket(this.entityId, effectInstance)
                } else {
                    // remove the effect
                    ClientboundRemoveMobEffectPacket(this.entityId, MobEffects.DIG_SLOWDOWN)
                }

            PacketHandler.sendPacket(this, packet)
        }

        private fun Player.sendBlockDestructionPacket(
            location: Location,
            stage: Int,
        ) {
            val packet = ClientboundBlockDestructionPacket(0, BlockPos(location.blockX, location.blockY, location.blockZ), stage)
            PacketHandler.sendPacket(this, packet)
        }

        // TODO: Using static methods when these are only used from one class... wrong design
        fun startBreaking(
            player: Player,
            location: Location,
            block: Block,
            item: ItemStack,
        ) {
            val damage = block.getBlockData().breakSpeed(item, player)
            val destroying = Destroying(0f, location, damage, block, item)
            player.disableBreakingAnimation()
            handler.blocks[player] = destroying
        }

        fun stopBreaking(player: Player) {
            player.enableBreakingAnimation()
            if (!handler.blocks.containsKey(player)) return
            player.sendBlockDestructionPacket(handler.blocks[player]!!.location, -1)
            handler.blocks.remove(player)
        }
    }
}
