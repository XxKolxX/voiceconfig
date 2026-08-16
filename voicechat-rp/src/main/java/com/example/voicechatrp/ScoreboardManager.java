package com.example.voicechatrp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ScoreboardManager {

    private static final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private static final Set<UUID> currentlyTalking = new HashSet<>();

    public static void setTalking(Player player, boolean talking) {
        UUID uuid = player.getUniqueId();
        boolean changed = false;

        if (talking) {
            if (currentlyTalking.add(uuid)) changed = true;
        } else {
            if (currentlyTalking.remove(uuid)) changed = true;
        }

        if (changed) {
            updateAllScoreboards();
        }
    }

    public static void updateAllScoreboards() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Double freq = RadioManager.getActiveFrequency(p);
            if (freq != null) {
                updateScoreboard(p, freq);
            } else {
                removeScoreboard(p);
            }
        }
    }

    private static void updateScoreboard(Player player, double frequency) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = playerScoreboards.computeIfAbsent(player.getUniqueId(), k -> manager.getNewScoreboard());

        Objective obj = board.getObjective("radio");
        if (obj == null) {
            obj = board.registerNewObjective("radio", Criteria.DUMMY, Component.text("Radio: " + frequency + " MHz", NamedTextColor.GOLD));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(Component.text("Radio: " + frequency + " MHz", NamedTextColor.GOLD));
        }

        // Avoid flickering by using teams instead of resetting scores
        int score = 0;
        Set<String> validEntries = new HashSet<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Double pFreq = RadioManager.getActiveFrequency(p);
            if (pFreq != null && pFreq.equals(frequency)) {
                String entryName = "§0§" + Math.abs(p.getName().hashCode() % 10) + p.getName().substring(0, Math.min(p.getName().length(), 10)); // Unique invisible prefix
                validEntries.add(entryName);

                Team team = board.getTeam(p.getName());
                if (team == null) {
                    team = board.registerNewTeam(p.getName());
                    team.addEntry(entryName);
                }

                String prefix = currentlyTalking.contains(p.getUniqueId()) ? "§a\u25B6 " : "§7\u25A0 ";
                team.prefix(Component.text(prefix + p.getName()));

                obj.getScore(entryName).setScore(score--);
            }
        }

        // Remove stale entries
        for (String entry : board.getEntries()) {
            if (!validEntries.contains(entry)) {
                board.resetScores(entry);
            }
        }

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    public static void removeScoreboard(Player player) {
        if (playerScoreboards.containsKey(player.getUniqueId())) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            playerScoreboards.remove(player.getUniqueId());
        }
    }
}
