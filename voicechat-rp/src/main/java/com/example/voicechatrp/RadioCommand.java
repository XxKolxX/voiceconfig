package com.example.voicechatrp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RadioCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracz moze uzywac tej komendy.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Uzycie: /radio <give|set|off> [czestotliwosc]", NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "give":
                ItemStack radioItem = new ItemStack(Material.COMPASS);
                ItemMeta meta = radioItem.getItemMeta();
                meta.displayName(Component.text("Krotkofalowka", NamedTextColor.DARK_GRAY));
                meta.getPersistentDataContainer().set(RadioManager.getFrequencyKey(), PersistentDataType.BYTE, (byte) 1);
                radioItem.setItemMeta(meta);
                player.getInventory().addItem(radioItem);
                player.sendMessage(Component.text("Otrzymales krotkofalowke!", NamedTextColor.GREEN));
                break;

            case "set":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Podaj czestotliwosc. (np. 100.5)", NamedTextColor.RED));
                    return true;
                }
                try {
                    double freq = Double.parseDouble(args[1]);
                    RadioManager.setFrequency(player, freq);
                    player.sendMessage(Component.text("Ustawiono czestotliwosc radia na " + freq + " MHz", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Nieprawidlowa czestotliwosc.", NamedTextColor.RED));
                }
                break;

            case "off":
                RadioManager.disableRadio(player);
                player.sendMessage(Component.text("Wylaczono radio.", NamedTextColor.YELLOW));
                break;

            default:
                player.sendMessage(Component.text("Nieznana akcja.", NamedTextColor.RED));
                break;
        }

        return true;
    }
}
