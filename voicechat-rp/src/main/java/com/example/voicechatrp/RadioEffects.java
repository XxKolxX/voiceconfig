package com.example.voicechatrp;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class RadioEffects {

    private static final Set<UUID> currentlyTalkingRadios = new HashSet<>();

    public static void updateTalkingState(Player player, boolean isTalking) {
        UUID uuid = player.getUniqueId();

        if (isTalking) {
            if (!currentlyTalkingRadios.contains(uuid)) {
                currentlyTalkingRadios.add(uuid);
                playRadioStartSound(player);
                ScoreboardManager.setTalking(player, true);
            }
        } else {
            if (currentlyTalkingRadios.contains(uuid)) {
                currentlyTalkingRadios.remove(uuid);
                playRadioEndSound(player);
                ScoreboardManager.setTalking(player, false);
            }
        }
    }

    private static void playRadioStartSound(Player sender) {
        Double freq = RadioManager.getActiveFrequency(sender);
        if (freq == null) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            Double pFreq = RadioManager.getActiveFrequency(p);
            if (pFreq != null && pFreq.equals(freq)) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
            }
        }
    }

    private static void playRadioEndSound(Player sender) {
        Double freq = RadioManager.getActiveFrequency(sender);
        if (freq == null) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            Double pFreq = RadioManager.getActiveFrequency(p);
            if (pFreq != null && pFreq.equals(freq)) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 2.0f);
            }
        }
    }

    // Cleanup if player quits
    public static void removePlayer(Player player) {
        if (currentlyTalkingRadios.contains(player.getUniqueId())) {
            updateTalkingState(player, false);
        }
    }
}
