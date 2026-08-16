package com.example.voicechatrp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttenuationManager {

    // senderUUID -> listenerUUID -> Location data and Multiplier
    private static final Map<UUID, Map<UUID, AttenuationData>> cache = new ConcurrentHashMap<>();
    private static final Set<UUID> trackedListeners = ConcurrentHashMap.newKeySet();

    public static void startTask() {
        Bukkit.getScheduler().runTaskTimer(VoiceChatRPPlugin.getInstance(), () -> {
            Set<UUID> currentPlayers = new HashSet<>();

            for (Player sender : Bukkit.getOnlinePlayers()) {
                currentPlayers.add(sender.getUniqueId());
                Map<UUID, AttenuationData> listenerMap = cache.computeIfAbsent(sender.getUniqueId(), k -> new ConcurrentHashMap<>());
                Location senderLoc = sender.getLocation();

                for (Player listener : Bukkit.getOnlinePlayers()) {
                    if (sender.equals(listener)) continue;

                    if (senderLoc.getWorld() != listener.getLocation().getWorld()) {
                        listenerMap.remove(listener.getUniqueId());
                        continue;
                    }

                    double dist = senderLoc.distance(listener.getLocation());
                    if (dist > 30.0) {
                        listenerMap.remove(listener.getUniqueId());
                        continue;
                    }

                    // Main thread: calculate physical block attenuation
                    double multiplier = SoundPhysics.calculateAttenuationSync(senderLoc, listener.getLocation(), 30.0);
                    listenerMap.put(listener.getUniqueId(), new AttenuationData(senderLoc.clone(), listener.getLocation().clone(), multiplier));
                }
            }

            trackedListeners.retainAll(currentPlayers);
            trackedListeners.addAll(currentPlayers);

        }, 10L, 10L); // Update every 10 ticks (0.5s)
    }

    public static AttenuationData getAttenuationData(UUID sender, UUID listener) {
        Map<UUID, AttenuationData> listenerMap = cache.get(sender);
        if (listenerMap != null) {
            return listenerMap.get(listener);
        }
        return null;
    }

    public static Collection<UUID> getAllTrackedListeners() {
        return trackedListeners;
    }

    public static void cleanup(UUID uuid) {
        cache.remove(uuid);
        trackedListeners.remove(uuid);
        for (Map<UUID, AttenuationData> map : cache.values()) {
            map.remove(uuid);
        }
    }

    public static class AttenuationData {
        public final Location senderLoc;
        public final Location listenerLoc;
        public final double multiplier;

        public AttenuationData(Location senderLoc, Location listenerLoc, double multiplier) {
            this.senderLoc = senderLoc;
            this.listenerLoc = listenerLoc;
            this.multiplier = multiplier;
        }
    }
}
