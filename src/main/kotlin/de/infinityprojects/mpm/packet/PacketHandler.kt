package de.infinityprojects.mpm.packet

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.protocol.Packet
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaType

object PacketHandler : Listener {
    val PACKET_PREFIX = "mpm"

    // Bukkit event handlers

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        injectPlayer(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        removePlayer(event.player)
    }

    // Listener registration

    private val listeners: HashSet<Listener> = hashSetOf()

    fun registerListeners(listener: Any) {
        listeners.addAll(
            listener::class.functions.mapNotNull { function ->
                function.findAnnotation<PacketListener>()?.let { packetListener ->
                    if (function.parameters.size != 2) {
                        throw IllegalArgumentException(
                            "@PacketListener annotation must be applied to a function with exactly one parameter",
                        )
                    }

                    val parameter = function.parameters[1]
                    val parameterType = parameter.type
                    val classifier = parameterType.classifier as? KClass<*>

                    if (classifier == null || !classifier.isSubclassOf(PacketEvent::class)) {
                        throw IllegalArgumentException(
                            "@PacketListener annotation must be applied to a function with a PacketEvent<T> parameter",
                        )
                    }

                    val javaType = parameterType.javaType
                    if (javaType is ParameterizedType) {
                        val typeArgument = javaType.actualTypeArguments.first()
                        if (typeArgument is Class<*>) {
                            return@mapNotNull Listener(typeArgument.kotlin, function, listener, packetListener)
                        } else {
                            throw IllegalArgumentException(
                                "PacketEvent<T> parameter must have a valid T. T must implement the interface Packet<*>.",
                            )
                        }
                    } else {
                        throw IllegalArgumentException(
                            "PacketEvent parameter must be a PacketEvent<T>, technically called a ParametrizedType",
                        )
                    }
                }
                return@mapNotNull null
            },
        )
    }

    // Player packet sending

    fun sendPacket(
        player: Player,
        packet: Packet<*>,
    ) {
        (player as CraftPlayer).handle.connection.send(packet)
    }

    // Packet handling

    /**
     * Injects a packet listener to a player.
     * @param player The player to be injected with
     */
    private fun injectPlayer(player: Player) {
        val channelDuplexHandler: ChannelDuplexHandler =
            object : ChannelDuplexHandler() {
                override fun channelRead(
                    ctx: ChannelHandlerContext,
                    msg: Any,
                ) {
                    val e = handlePacket(msg, player)
                    if (e == null) {
                        super.channelRead(ctx, msg)
                    } else if (!e.cancelled) {
                        super.channelRead(ctx, e.packet)
                    }
                }

                override fun write(
                    ctx: ChannelHandlerContext?,
                    msg: Any?,
                    promise: ChannelPromise?,
                ) {
                    if (msg != null) {
                        val e = handlePacket(msg, player)
                        if (e == null) {
                            super.write(ctx, msg, promise)
                        } else if (!e.cancelled) {
                            super.write(ctx, e.packet, promise)
                        }
                    }
                }
            }

        val pipeline = (player as CraftPlayer).handle.connection.connection.channel.pipeline()
        pipeline.addBefore("packet_handler", "$PACKET_PREFIX:${player.name}", channelDuplexHandler)
    }

    /**
     * Removes the injected packet listener from the player
     * @param player Player to be removed from the listeners
     */

    private fun removePlayer(player: Player) {
        val channel = (player as CraftPlayer).handle.connection.connection.channel
        channel.eventLoop().submit {
            channel.pipeline().remove("$PACKET_PREFIX:${player.name}")
        }
    }

    /**
     * Sends a packet to its specific listeners
     * @param packet The packet to be sent
     * @return Whether the packet should continue or not (false = cancelled)
     */
    private fun handlePacket(
        packet: Any,
        player: Player,
    ): PacketEvent<*>? {
        if (packet is Packet<*>) {
            val packetEvent = PacketEvent(packet, player)
            listeners.sortedByDescending { it.packetListener.priority.ordinal }
                .filter { it.listeningToPacket.isInstance(packet) }
                .forEach {
                    if (!it.packetListener.ignoreCancelled || !packetEvent.cancelled) {
                        it.method.call(it.instance, packetEvent)
                    }
                }

            return packetEvent
        }
        return null
    }

    // Lifecycle methods

    fun enable(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getOnlinePlayers().forEach(::injectPlayer)
    }

    fun destroy() {
        Bukkit.getOnlinePlayers().forEach(::removePlayer)
    }

    // Listener data class
    data class Listener(
        val listeningToPacket: KClass<*>,
        val method: KFunction<*>,
        val instance: Any,
        val packetListener: PacketListener,
    )
}
