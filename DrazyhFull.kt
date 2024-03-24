class FakeLagDisabler : DisablerMode("DrazyhFull") {
    class Timer {
        private var lastExecutionTime: Long = 0

        fun reset() {
            lastExecutionTime = System.currentTimeMillis()
        }

        fun check(interval: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastExecutionTime
            return elapsedTime >= interval
        }
    }

    class PacketHandler {
        private val packets: MutableList<Packet<*>> = Collections.synchronizedList(ArrayList())
        private val packetTimer: Timer = Timer()
        private val skippedTicks: Timer = Timer()

        fun onEnable() {
            packetTimer.reset()
            skippedTicks.reset()
        }

        fun onDisable() {
            packets.clear()
        }


        fun handle(event: MotionEvent) {
            if (Minecraft.getMinecraft().thePlayer.ticksExisted % 20 == 0) {
                PacketUtils.sendPacketNoEvent(C0CPacketInput())
            }
        }


        fun onPacket(event: PacketEvent) {
            if (packetTimer.check(1000L * 20L)) {
                packetTimer.reset()

                synchronized(packets) {
                    packets.forEach {
                        packets.remove(it)

                        if (it !is S00PacketKeepAlive && it !is S32PacketConfirmTransaction) {
                            PacketUtils.sendPacketNoEvent(it as Packet<INetHandlerPlayServer>)
                        }
                    }
                }

                packets.clear()
            }

            if (skippedTicks.check(1000L)) {
                skippedTicks.reset()
            }

            val packet = event.packet

            if (packet is C03PacketPlayer) {
                if (!skippedTicks.check(100L)) {
                    event.cancelEvent()
                }
            }

            if (packet is C0CPacketInput || packet is C00PacketKeepAlive || packet is C0FPacketConfirmTransaction) {
                event.cancelEvent()

                synchronized(packets) {
                    packets.add(packet)
                }
            }
        }
    }



}