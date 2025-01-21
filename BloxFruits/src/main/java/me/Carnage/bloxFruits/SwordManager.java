package me.Carnage.bloxFruits;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SwordManager {
    private final FileConfiguration config;

    public SwordManager(FileConfiguration config) {
        this.config = config;
    }

    public List<String> getSwordsByRarity(String rarity) {
        List<String> swords = new ArrayList<>();
        if (config.isConfigurationSection("swords")) {
            config.getConfigurationSection("swords").getKeys(false).forEach(sword -> {
                String swordRarity = config.getString("swords." + sword + ".rarity");
                if (swordRarity != null && swordRarity.equalsIgnoreCase(rarity)) {
                    swords.add(sword);
                }
            });
        } else {
            Bukkit.getLogger().severe("'swords' section is missing in the configuration!");
        }
        return swords;
    }

    public ItemStack createSwordItem(String swordKey) {
        String name = config.getString("swords." + swordKey + ".name");
        String itemType = config.getString("swords." + swordKey + ".item");
        List<String> description = config.getStringList("swords." + swordKey + ".description");

        Material material = Material.matchMaterial(itemType);
        if (material == null) {
            Bukkit.getLogger().severe("Invalid material for sword: " + swordKey);
            material = Material.WOODEN_SWORD; // Default material
        }

        ItemStack sword = new ItemStack(material);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            description.forEach(line -> lore.add(ChatColor.translateAlternateColorCodes('&', line)));
            meta.setLore(lore);
            sword.setItemMeta(meta);
        }

        return sword;
    }
}
