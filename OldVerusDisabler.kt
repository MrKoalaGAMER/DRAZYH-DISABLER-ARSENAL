package net.ccbluex.liquidbounce.features.module.modules.exploit.disablers.verus

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.modules.exploit.disablers.DisablerMode
import net.ccbluex.liquidbounce.ui.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import java.util.*
import kotlin.math.sqrt

class OldVerus : DisablerMode("OldVerus") {
    private var verus2Stat = false
    private val verusSlientFlagApplyValue = BoolValue("OldVerus-SlientFlagApply", false)
    private val verusBufferSizeValue = IntegerValue("OldVerus-BufferSize", 300, 0, 1000)
    private val verusRepeatTimesValue = IntegerValue("OldVerus-RepeatTimes", 1, 1, 5)
    private val verusRepeatTimesFightingValue = IntegerValue("OldVerus-RepeatTimesFighting", 1, 1, 5)
    private val verusFlagDelayValue = IntegerValue("OldVerus-FlagDelay", 40, 35, 60)
    private var lagTimer = MSTimer()
    private var modified = false
    private val repeatTimes: Int
        get() = if(FDPClient.combatManager.inCombat) { verusRepeatTimesFightingValue.get() } else { verusRepeatTimesValue.get() }
    private val packetBuffer = LinkedList<Packet<INetHandlerPlayServer>>()
    override fun onEnable() {
        verus2Stat = false
        lagTimer.reset()
        modified = false
        packetBuffer.clear()
    }
    override fun onWorld(event: WorldEvent) {
        verus2Stat = false
        packetBuffer.clear()
        lagTimer.reset()
    }


    override fun onUpdate(event: UpdateEvent) {
        modified = false
        if(lagTimer.hasTimePassed(490L)) {
            lagTimer.reset()
            if(packetBuffer.isNotEmpty()) {
                val packet = packetBuffer.poll()
                repeat(repeatTimes) {
                    PacketUtils.sendPacketNoEvent(packet)
                }
                disabler.debugMessage("Send Packet Buff")
            } else {
                disabler.debugMessage("Empty Packet Buff")
            }
        }
    }
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if(packet is C0FPacketConfirmTransaction) {
            packetBuffer.add(packet)
            event.cancelEvent()
            if(packetBuffer.size > verusBufferSizeValue.get()) {
                if(!verus2Stat) {
                    verus2Stat = true
                    FDPClient.hud.addNotification(Notification(disabler.name, "AntiCheat is disabled.", NotifyType.SUCCESS))
                }
                val packeted = packetBuffer.poll()
                repeat(repeatTimes) {
                    PacketUtils.sendPacketNoEvent(packeted)
                }
            }
            disabler.debugMessage("Packet C0F IN ${packetBuffer.size}")
        } else if(packet is C03PacketPlayer) {
            if((mc.thePlayer.ticksExisted % verusFlagDelayValue.get() == 0) && (mc.thePlayer.ticksExisted > verusFlagDelayValue.get() + 1) && !modified) {
                disabler.debugMessage("Packet C03")
                modified = true
                packet.y -= 11.4514 // 逸一时，误一世
                packet.onGround = false
            }
        } else if (packet is S08PacketPlayerPosLook && verusSlientFlagApplyValue.get()) {
            val x = packet.x - mc.thePlayer.posX
            val y = packet.y - mc.thePlayer.posY
            val z = packet.z - mc.thePlayer.posZ
            val diff = sqrt(x * x + y * y + z * z)
            if (diff <= 8) {
                event.cancelEvent()
                disabler.debugMessage("Silent Flag")
                PacketUtils.sendPacketNoEvent(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        packet.x,
                        packet.y,
                        packet.z,
                        packet.getYaw(),
                        packet.getPitch(),
                        true
                    )
                )
            }
        }

        if (mc.thePlayer != null && mc.thePlayer.ticksExisted <= 7) {
            lagTimer.reset()
            packetBuffer.clear()
        }
    }
}