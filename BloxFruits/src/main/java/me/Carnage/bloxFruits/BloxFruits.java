package me.Carnage.bloxFruits;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BloxFruits extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        // Register commands and events
        this.getCommand("bloxfruits").setExecutor(new BloxFruitsCommand(config));

        getServer().getPluginManager().registerEvents(new KatanaAbilities(config), this);
        getServer().getPluginManager().registerEvents(new CutlassAbilities(config), this);
        getServer().getPluginManager().registerEvents(new DualKatanaAbilities(config), this);
        getServer().getPluginManager().registerEvents(new TripleKatanaAbilities(config), this);
        getServer().getPluginManager().registerEvents(new SharkSawAbilities(config), this);

        getLogger().info("BloxFruits plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BloxFruits plugin has been disabled.");
    }
}
