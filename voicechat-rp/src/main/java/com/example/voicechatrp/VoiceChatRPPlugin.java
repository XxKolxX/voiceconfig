package com.example.voicechatrp;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class VoiceChatRPPlugin extends JavaPlugin implements Listener {

    private static VoiceChatRPPlugin instance;
    private RPVoiceChatPlugin voicechatPlugin;

    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();
        logger.info("Enabling VoiceChatRP...");

        getCommand("radio").setExecutor(new RadioCommand());
        getServer().getPluginManager().registerEvents(this, this);

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new RPVoiceChatPlugin();
            service.registerPlugin(voicechatPlugin);
            logger.info("Successfully registered Simple Voice Chat plugin.");
        } else {
            logger.severe("Simple Voice Chat service not found! Is the plugin installed?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AttenuationManager.startTask();

        // Refresh radios periodically
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                RadioManager.getActiveFrequency(p);
            }
        }, 20L, 40L);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling VoiceChatRP...");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ScoreboardManager.updateAllScoreboards();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        RadioEffects.removePlayer(event.getPlayer());
        RadioManager.disableRadio(event.getPlayer());
        ScoreboardManager.removeScoreboard(event.getPlayer());
        AttenuationManager.cleanup(event.getPlayer().getUniqueId());
    }

    public static VoiceChatRPPlugin getInstance() {
        return instance;
    }
}
