package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.ProtocolInfoBuilder;

public class HandshakeProtocols {
    public static final ProtocolInfo<ServerHandshakePacketListener> SERVERBOUND = ProtocolInfoBuilder.serverboundProtocol(
        ConnectionProtocol.HANDSHAKING, p_328992_ -> p_328992_.addPacket(HandshakePacketTypes.CLIENT_INTENTION, ClientIntentionPacket.STREAM_CODEC)
    );
}