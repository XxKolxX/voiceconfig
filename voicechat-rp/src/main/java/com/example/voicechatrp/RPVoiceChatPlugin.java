package com.example.voicechatrp;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.Position;

import java.util.UUID;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class RPVoiceChatPlugin implements VoicechatPlugin {

    public static VoicechatApi voicechatApi;
    public static VoicechatServerApi voicechatServerApi;

    // UUID -> Last time spoken (ms)
    private static final Map<UUID, Long> lastSpokenTimes = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "voicechatrp";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        voicechatServerApi = event.getVoicechat();

        // Start a task to check for radio end sounds. Main thread safe.
        org.bukkit.Bukkit.getScheduler().runTaskTimer(VoiceChatRPPlugin.getInstance(), () -> {
            long now = System.currentTimeMillis();
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                Long lastSpoken = lastSpokenTimes.get(p.getUniqueId());
                if (lastSpoken != null && now - lastSpoken > 300) {
                    RadioEffects.updateTalkingState(p, false);
                    lastSpokenTimes.remove(p.getUniqueId());
                }
            }
        }, 5L, 5L);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (voicechatServerApi == null) return;
        if (event.getSenderConnection() == null) return;

        // This is executing on an ASYNC thread (UDP). Do not call Bukkit APIs!

        event.cancel();

        UUID senderUuid = event.getSenderConnection().getPlayer().getUuid();
        double maxDistance = 30.0;
        MicrophonePacket micPacket = (MicrophonePacket) event.getPacket();

        Double senderFreq = RadioManager.getActiveFrequencyForUUID(senderUuid);
        boolean usingRadio = (senderFreq != null);

        if (usingRadio) {
            lastSpokenTimes.put(senderUuid, System.currentTimeMillis());
            org.bukkit.Bukkit.getScheduler().runTask(VoiceChatRPPlugin.getInstance(), () -> {
                org.bukkit.entity.Player senderPlayer = org.bukkit.Bukkit.getPlayer(senderUuid);
                if (senderPlayer != null) {
                    RadioEffects.updateTalkingState(senderPlayer, true);
                }
            });
        }

        // We can use AttenuationManager's cache to know who is online and where they are
        for (UUID listenerUuid : AttenuationManager.getAllTrackedListeners()) {
            if (listenerUuid.equals(senderUuid)) continue;

            VoicechatConnection connection = voicechatServerApi.getConnectionOf(listenerUuid);
            if (connection == null) continue;

            // Handle Radio Logic First
            if (usingRadio) {
                Double listenerFreq = RadioManager.getActiveFrequencyForUUID(listenerUuid);
                if (listenerFreq != null && listenerFreq.equals(senderFreq)) {
                    StaticSoundPacket radioPacket = micPacket.staticSoundPacketBuilder().build();
                    voicechatServerApi.sendStaticSoundPacketTo(connection, radioPacket);
                    continue;
                }
            }

            // Fallback to 3D Physical Positional Audio using cached sync data
            AttenuationManager.AttenuationData attenData = AttenuationManager.getAttenuationData(senderUuid, listenerUuid);
            if (attenData == null) continue; // Out of range or different world

            if (attenData.multiplier > 0.05) {
                double simulatedDistance = maxDistance * (1.0 - attenData.multiplier);
                org.bukkit.util.Vector dir = attenData.senderLoc.toVector().subtract(attenData.listenerLoc.toVector());
                if (dir.lengthSquared() > 0) {
                    dir.normalize();
                }
                org.bukkit.util.Vector simulatedLocVec = attenData.senderLoc.toVector().add(dir.multiply(simulatedDistance));

                Position simulatedPos = voicechatServerApi.createPosition(simulatedLocVec.getX(), simulatedLocVec.getY(), simulatedLocVec.getZ());

                LocationalSoundPacket simulatedPacket = micPacket.locationalSoundPacketBuilder()
                        .position(simulatedPos)
                        .distance((float) maxDistance)
                        .build();

                voicechatServerApi.sendLocationalSoundPacketTo(connection, simulatedPacket);
            }
        }
    }
}
