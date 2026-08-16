package com.example.voicechatrp;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RadioManager {

    private static final NamespacedKey FREQUENCY_KEY = new NamespacedKey(VoiceChatRPPlugin.getInstance(), "radio_frequency");

    // UUID -> Frequency
    private static final Map<UUID, Double> activeRadios = new ConcurrentHashMap<>();

    public static void setFrequency(Player player, double frequency) {
        activeRadios.put(player.getUniqueId(), frequency);
        ScoreboardManager.updateAllScoreboards();
    }

    public static void disableRadio(Player player) {
        activeRadios.remove(player.getUniqueId());
        ScoreboardManager.updateAllScoreboards();
    }

    public static Double getActiveFrequency(Player player) {
        if (!hasRadioItem(player)) {
            if (activeRadios.remove(player.getUniqueId()) != null) {
                ScoreboardManager.updateAllScoreboards();
            }
            return null;
        }
        return activeRadios.get(player.getUniqueId());
    }

    // Thread-safe fetch for UDP network thread
    public static Double getActiveFrequencyForUUID(UUID uuid) {
        return activeRadios.get(uuid);
    }

    private static boolean hasRadioItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
                if (container.has(FREQUENCY_KEY, PersistentDataType.BYTE)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static NamespacedKey getFrequencyKey() {
        return FREQUENCY_KEY;
    }
}
