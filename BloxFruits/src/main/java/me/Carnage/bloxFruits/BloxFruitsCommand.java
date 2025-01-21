package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class BloxFruitsCommand implements CommandExecutor {
    private final SwordManager swordManager;
    private final FileConfiguration config;

    public BloxFruitsCommand(FileConfiguration config) {
        this.swordManager = new SwordManager(config);
        this.config = config; // Save the config reference
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /bloxfruits <sword rarity>").color(NamedTextColor.RED));
            return true;
        }

        String rarity = args[0].toLowerCase();
        List<String> swords = swordManager.getSwordsByRarity(rarity);

        if (swords.isEmpty()) {
            player.sendMessage(Component.text("No swords found for the rarity: " + rarity).color(NamedTextColor.RED));
            return true;
        }

        openSwordGUI(player, rarity, swords);
        return true;
    }

    private void openSwordGUI(Player player, String rarity, List<String> swords) {
        List<List<Integer>> layout = getLayout(rarity);
        if (layout.isEmpty()) {
            player.sendMessage(Component.text("No layout defined for rarity: " + rarity).color(NamedTextColor.RED));
            Bukkit.getLogger().severe("No layout found for rarity: " + rarity);
            return;
        }

        int inventorySize = layout.size() * 9; // Each row in layout represents 9 slots
        Inventory gui = Bukkit.createInventory(null, inventorySize, Component.text(rarity + " Swords").color(NamedTextColor.BLUE));

        int swordIndex = 0;
        for (int row = 0; row < layout.size(); row++) {
            List<Integer> currentRow = layout.get(row);
            for (int col = 0; col < currentRow.size(); col++) {
                int slot = row * 9 + col;
                if (currentRow.get(col) == 1) { // Place sword in slot if 1
                    if (swordIndex < swords.size()) {
                        gui.setItem(slot, swordManager.createSwordItem(swords.get(swordIndex)));
                        swordIndex++;
                    }
                }
            }
        }

        Bukkit.getLogger().info("Opened GUI for rarity " + rarity + " with " + swordIndex + " swords.");
        player.openInventory(gui);
    }


    private List<List<Integer>> getLayout(String rarity) {
        List<?> rawLayout = config.getList("gui.layouts." + rarity);
        List<List<Integer>> layout = new ArrayList<>();

        if (rawLayout != null) {
            for (Object row : rawLayout) {
                if (row instanceof List<?>) {
                    List<?> rawRow = (List<?>) row;
                    List<Integer> parsedRow = new ArrayList<>();
                    for (Object cell : rawRow) {
                        if (cell instanceof Integer) {
                            parsedRow.add((Integer) cell);
                        } else {
                            parsedRow.add(0); // Default to empty space if cell is not an Integer
                        }
                    }
                    layout.add(parsedRow);
                }
            }
        }

        return layout;
    }
}
