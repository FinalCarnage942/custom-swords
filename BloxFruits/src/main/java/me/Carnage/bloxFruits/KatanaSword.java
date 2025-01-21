package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class KatanaSword {

    private final FileConfiguration config;

    public KatanaSword(FileConfiguration config) {
        this.config = config;
    }

    public void giveKatana(Player player) {
        // Retrieve the katana properties from the config
        String name = config.getString("swords.katana.name");
        String itemType = config.getString("swords.katana.item");
        List<String> description = config.getStringList("swords.katana.description");

        // Get the item material from the config
        Material material = Material.matchMaterial(itemType);
        if (material == null) {
            player.sendMessage(Component.text("Invalid item type in the configuration for Katana!").color(NamedTextColor.RED));
            return;
        }

        // Create the katana item
        ItemStack katana = new ItemStack(material);
        ItemMeta meta = katana.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(description.stream().map(Component::text).toList());
            katana.setItemMeta(meta);
        }

        // Give the katana to the player
        player.getInventory().addItem(katana);
    }
}
