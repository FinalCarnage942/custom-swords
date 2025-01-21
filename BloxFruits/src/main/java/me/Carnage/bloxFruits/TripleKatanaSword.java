package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TripleKatanaSword {

    private final FileConfiguration config;

    public TripleKatanaSword(FileConfiguration config) {
        this.config = config;
    }

    public void giveTripleKatana(Player player) {
        // Retrieve the Triple Katana properties from the config
        String name = config.getString("swords.triple_katana.name");
        String itemType = config.getString("swords.triple_katana.item");
        List<String> description = config.getStringList("swords.triple_katana.description");

        // Get the item material from the config
        Material material = Material.matchMaterial(itemType);
        if (material == null) {
            player.sendMessage(Component.text("Invalid item type in the configuration for Triple Katana!").color(NamedTextColor.RED));
            return;
        }

        // Create the Triple Katana item
        ItemStack tripleKatana = new ItemStack(material);
        ItemMeta meta = tripleKatana.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name).color(NamedTextColor.GOLD));
            meta.lore(description.stream().map(line -> Component.text(line).color(NamedTextColor.GRAY)).toList());
            tripleKatana.setItemMeta(meta);
        }

        // Give the Triple Katana to the player
        player.getInventory().addItem(tripleKatana);
        player.sendMessage(Component.text("You have received the Triple Katana!").color(NamedTextColor.YELLOW));
    }
}
