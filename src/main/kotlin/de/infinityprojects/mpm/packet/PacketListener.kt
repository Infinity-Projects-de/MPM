package de.infinityprojects.mpm.packet

import org.bukkit.event.EventPriority

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PacketListener(
    /**
     * Define the priority of the event.
     * <p>
     * First priority to the last priority executed:
     * <ol>
     * <li>LOWEST
     * <li>LOW
     * <li>NORMAL
     * <li>HIGH
     * <li>HIGHEST
     * <li>MONITOR
     * </ol>
     *
     * @return the priority
     */
    val priority: EventPriority = EventPriority.NORMAL,
    val ignoreCancelled: Boolean = false
)