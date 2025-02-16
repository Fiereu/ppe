package de.fiereu.ppe.forms;

import de.fiereu.ppe.proxy.Direction;
import de.fiereu.ppe.proxy.ProxyClient;
import lombok.Getter;

@Getter
public class PacketInstance {
    private final Direction direction;
    private final ProxyClient client;
    private final int packetId;
    private final byte[] data;

    public PacketInstance(Direction direction, ProxyClient client, int packetId, byte[] data) {
        this.direction = direction;
        this.client = client;
        this.packetId = packetId;
        this.data = data;
    }

    public void send() {
        if (client != null && !client.isClosed()) {
            client.sendPacket(packetId, direction, data);
        }
    }
}
