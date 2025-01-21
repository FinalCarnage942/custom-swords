package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SharkSawSword {

    private final FileConfiguration config;

    public SharkSawSword(FileConfiguration config) {
        this.config = config;
    }

    public void giveSharkSaw(Player player) {
        // Retrieve the Shark Saw properties from the config
        String name = config.getString("swords.shark_saw.name");
        String itemType = config.getString("swords.shark_saw.item");
        List<String> description = config.getStringList("swords.shark_saw.description");

        // Get the item material from the config
        Material material = Material.matchMaterial(itemType);
        if (material == null) {
            player.sendMessage(Component.text("Invalid item type in the configuration for Shark Saw!").color(NamedTextColor.RED));
            return;
        }

        // Create the Shark Saw item
        ItemStack sharkSaw = new ItemStack(material);
        ItemMeta meta = sharkSaw.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(description.stream().map(Component::text).toList());
            sharkSaw.setItemMeta(meta);
        }

        // Give the Shark Saw to the player
        player.getInventory().addItem(sharkSaw);

        // Notify the player
        player.sendMessage(Component.text("You have received the Shark Saw!").color(NamedTextColor.GREEN));
    }
}