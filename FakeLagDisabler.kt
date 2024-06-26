package net.ccbluex.liquidbounce.features.module.modules.exploit.disablers.other

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.modules.exploit.disablers.DisablerMode
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.utils.BlinkUtils

class FakeLagDisabler : DisablerMode("FakeLag") {
    private val c0FPacketValue = BoolValue("${valuePrefix}C0FPacket", false)
    private val c00PacketValue = BoolValue("${valuePrefix}C00Packet", false)
    private val lagDelayValue = IntegerValue("${valuePrefix}LagDelay", 0, 0, 2000)
    private val lagDurationValue = IntegerValue("${valuePrefix}LagDuration", 200, 100, 1000)
    private val fakeLagDelay = MSTimer()
    private val fakeLagDuration = MSTimer()
    override fun onEnable() {
        BlinkUtils.clearPacket()
        BlinkUtils.setBlinkState(all = true)
        fakeLagDuration.reset()
        fakeLagDelay.reset()
    }
    override fun onDisable() {
        BlinkUtils.setBlinkState(packetTransaction = c0FPacketValue.get() || BlinkUtils.transactionStat, packetKeepAlive = c00PacketValue.get() || BlinkUtils.keepAliveStat)
        BlinkUtils.releasePacket(onlySelected = true)
        BlinkUtils.setBlinkState(off = true)
    }

    override fun onWorld(event: WorldEvent) {
        BlinkUtils.clearPacket()
        BlinkUtils.setBlinkState(off = true)
        fakeLagDuration.reset()
        fakeLagDelay.reset()
    }

    override fun onUpdate(event: UpdateEvent) {
        if(FDPClient.moduleManager[Blink::class.java]!!.state) {
            fakeLagDelay.reset()
            fakeLagDuration.reset()
            return
        }
        if (mc.thePlayer.isDead) {
            BlinkUtils.setBlinkState(packetTransaction = c0FPacketValue.get() || BlinkUtils.transactionStat, packetKeepAlive = c00PacketValue.get() || BlinkUtils.keepAliveStat)
            BlinkUtils.releasePacket(onlySelected = true)
            BlinkUtils.setBlinkState(off = true)
            return
        }
        if (fakeLagDuration.hasTimePassed(lagDurationValue.get().toLong())) {
            fakeLagDelay.reset()
            fakeLagDuration.reset()
            disabler.debugMessage("Release buf(size=${BlinkUtils.bufferSize()})")
            BlinkUtils.setBlinkState(packetTransaction = c0FPacketValue.get() || BlinkUtils.transactionStat, packetKeepAlive = c00PacketValue.get() || BlinkUtils.keepAliveStat)
            BlinkUtils.releasePacket(onlySelected = true)
            BlinkUtils.setBlinkState(off = true)
        } else if (fakeLagDelay.hasTimePassed(lagDelayValue.get().toLong())) {
            BlinkUtils.setBlinkState(all = true)
        } else {
            fakeLagDuration.reset()
            BlinkUtils.setBlinkState(off = true)
        }
    }
}