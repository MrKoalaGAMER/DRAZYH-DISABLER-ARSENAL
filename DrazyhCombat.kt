class Drazyh : DisablerMode("Drazyh Combat") {

    @EventTarget
    fun handle(event: PacketEvent) {
        val packet: Packet<*> = event.packet

        if (mc.thePlayer.isDead) {
            return
        }

        if (packet is C03PacketPlayer) {
            if (mc.thePlayer.ticksExisted % 2 == 0) {
                mc.netHandler.addToSendQueue(
                    C0CPacketInput(
                        mc.thePlayer.moveStrafing, mc.thePlayer.moveForward,
                        mc.thePlayer.movementInput.jump, mc.thePlayer.movementInput.sneak
                    )
                )
            }
        }
    }
}