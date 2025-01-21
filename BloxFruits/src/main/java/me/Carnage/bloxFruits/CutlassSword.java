package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CutlassSword {

    private final FileConfiguration config;

    public CutlassSword(FileConfiguration config) {
        this.config = config;
    }

    public void giveCutlass(Player player) {
        // Retrieve the cutlass properties from the config
        String name = config.getString("swords.cutlass.name");
        String itemType = config.getString("swords.cutlass.item");
        List<String> description = config.getStringList("swords.cutlass.description");

        // Get the item material from the config
        Material material = Material.matchMaterial(itemType);
        if (material == null) {
            player.sendMessage(Component.text("Invalid item type in the configuration for Cutlass!").color(NamedTextColor.RED));
            return;
        }

        // Create the cutlass item
        ItemStack cutlass = new ItemStack(material);
        ItemMeta meta = cutlass.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(description.stream().map(Component::text).toList());
            cutlass.setItemMeta(meta);
        }

        // Give the cutlass to the player
        player.getInventory().addItem(cutlass);
    }
}
