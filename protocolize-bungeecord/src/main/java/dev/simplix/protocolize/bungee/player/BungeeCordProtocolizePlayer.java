package dev.simplix.protocolize.bungee.player;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.PlayerInventory;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.providers.ProtocolRegistrationProvider;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import dev.simplix.protocolize.bungee.packet.BungeeCordProtocolizePacket;
import dev.simplix.protocolize.bungee.util.ReflectionUtil;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.protocol.DefinedPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 26.08.2021
 *
 * @author Exceptionflug
 */
@Getter
@Accessors(fluent = true)
public class BungeeCordProtocolizePlayer implements ProtocolizePlayer {

    private static final ProtocolRegistrationProvider REGISTRATION_PROVIDER = Protocolize.protocolRegistration();
    private final AtomicInteger windowId = new AtomicInteger(101);
    private final Map<Integer, Inventory> registeredInventories = new ConcurrentHashMap<>();
    private final PlayerInventory proxyInventory = new PlayerInventory(this);
    private final UUID uniqueId;

    public BungeeCordProtocolizePlayer(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public void sendPacket(Object packet) {
        if (packet instanceof AbstractPacket) {
            BungeeCordProtocolizePacket pack = (BungeeCordProtocolizePacket) REGISTRATION_PROVIDER.createPacket((Class<? extends AbstractPacket>) packet.getClass(),
                    Protocol.PLAY, PacketDirection.CLIENTBOUND, ProtocolVersions.MINECRAFT_LATEST);
            pack.wrapper((AbstractPacket) packet);
            packet = pack;
        }
        ProxiedPlayer player = player();
        if (player != null) {
            player.unsafe().sendPacket((DefinedPacket) packet);
        }
    }

    @Override
    public void sendPacketToServer(Object packet) {
        if (packet instanceof AbstractPacket) {
            BungeeCordProtocolizePacket pack = (BungeeCordProtocolizePacket) REGISTRATION_PROVIDER.createPacket((Class<? extends AbstractPacket>) packet.getClass(),
                    Protocol.PLAY, PacketDirection.SERVERBOUND, ProtocolVersions.MINECRAFT_LATEST);
            pack.wrapper((AbstractPacket) packet);
            packet = pack;
        }
        ProxiedPlayer player = player();
        if (player != null) {
            Server server = player.getServer();
            if (server == null) {
                return; // Maybe in transition?? BungeeCord doesn't support on the fly connections like Velocity :/
            }
            server.unsafe().sendPacket((DefinedPacket) packet);
        }
    }

    @Override
    public int generateWindowId() {
        int out = windowId.incrementAndGet();
        if (out >= 200) {
            out = 101;
            windowId.set(101);
        }
        return out;
    }

    @Override
    public int protocolVersion() {
        return ReflectionUtil.getProtocolVersion(player());
    }

    private ProxiedPlayer player() {
        return ProxyServer.getInstance().getPlayer(uniqueId);
    }

}
