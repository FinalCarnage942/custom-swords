package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DualKatanaAbilities implements Listener {

    private final Map<UUID, Long> whirlwindCooldown = new HashMap<>();
    private final Map<UUID, Long> tornadoCooldown = new HashMap<>();
    private final FileConfiguration config;

    public DualKatanaAbilities(FileConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerUseDualKatana(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String itemType = config.getString("swords.dual_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null || item.getType() != material || item.getItemMeta() == null) {
            return;
        }

        String itemName = item.getItemMeta().getDisplayName();
        if (!itemName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.dual_katana.name")))) {
            return;
        }

        boolean isSneaking = player.isSneaking();
        Action action = event.getAction();

        // Sneak + Left-click for Whirlwind
        if (isSneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            handleWhirlwind(player);
        }

        // Sneak + Right-click for Tornado
        if (isSneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleTornado(player);
        }
    }

    @EventHandler
    public void onPlayerHoldDualKatana(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        String itemType = config.getString("swords.dual_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null || newItem == null || newItem.getType() != material || newItem.getItemMeta() == null) {
            // Remove the off-hand item if the player is no longer holding the Dual Katana
            if (offHandItem != null && offHandItem.getType() == material) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }

        String itemName = newItem.getItemMeta().getDisplayName();
        if (itemName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.dual_katana.name")))) {
            equipDualKatanas(player);
        }
    }

    @EventHandler
    public void onPlayerDropDualKatana(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        String itemType = config.getString("swords.dual_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null || droppedItem.getType() != material || droppedItem.getItemMeta() == null) {
            return;
        }

        String itemName = droppedItem.getItemMeta().getDisplayName();
        if (itemName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.dual_katana.name")))) {
            // Remove the off-hand sword
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the clicked slot is the off-hand slot
        if (event.getSlot() == EquipmentSlot.OFF_HAND.ordinal()) {
            Player player = (Player) event.getWhoClicked();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();

            String itemType = config.getString("swords.dual_katana.item");
            Material material = Material.matchMaterial(itemType);

            if (material != null && mainHandItem != null && mainHandItem.getType() == material) {
                String mainHandName = mainHandItem.getItemMeta().getDisplayName();
                if (mainHandName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.dual_katana.name")))) {
                    // Prevent interaction with the off-hand item
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot remove the off-hand katana while holding the Dual Katana.")
                            .color(NamedTextColor.RED));
                }
            }
        }
    }

    private void equipDualKatanas(Player player) {
        String itemType = config.getString("swords.dual_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null) {
            player.sendMessage(Component.text("Invalid item type in the configuration for Dual Katana!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Create the off-hand katana
        ItemStack offHand = createKatanaItem(material, config.getString("swords.dual_katana.name"));

        // Equip the player with the Dual Katana
        player.getInventory().setItemInOffHand(offHand);

        // Notify the player
        player.sendMessage(Component.text("You are now wielding the Dual Katana!")
                .color(NamedTextColor.GREEN));
    }

    private ItemStack createKatanaItem(Material material, String name) {
        ItemStack katana = new ItemStack(material);
        ItemMeta meta = katana.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            katana.setItemMeta(meta);
        }
        return katana;
    }

    private void handleWhirlwind(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.dual_katana.cooldowns.whirlwind", 5000);

        if (isOnCooldown(playerId, whirlwindCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, whirlwindCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Whirlwind cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        int damage = config.getInt("swords.dual_katana.damage.whirlwind", 10);
        executeWhirlwind(player, Particle.SWEEP_ATTACK, Particle.CRIT, damage);

        whirlwindCooldown.put(playerId, System.currentTimeMillis());
    }

    private void handleTornado(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.dual_katana.cooldowns.tornado", 7000);

        if (isOnCooldown(playerId, tornadoCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, tornadoCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Tornado cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        int damage = config.getInt("swords.dual_katana.damage.tornado", 15);
        executeTornado(player, Particle.CLOUD, Particle.SNOWFLAKE, damage);

        tornadoCooldown.put(playerId, System.currentTimeMillis());
    }

    private boolean isOnCooldown(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long currentTime = System.currentTimeMillis();
        return cooldownMap.containsKey(playerId) && (currentTime - cooldownMap.get(playerId)) < cooldownTime;
    }

    private long getCooldownRemaining(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long lastUseTime = cooldownMap.getOrDefault(playerId, 0L);
        return cooldownTime - (System.currentTimeMillis() - lastUseTime);
    }

    private void executeWhirlwind(Player player, Particle spawnParticle, Particle hitParticle, int damage) {
        player.sendMessage(Component.text("You used Whirlwind!", NamedTextColor.BLUE));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        for (int angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            double x = Math.cos(radians) * 3; // Radius 3 blocks
            double z = Math.sin(radians) * 3;
            Location particleLocation = player.getLocation().clone().add(x, 1, z);
            player.getWorld().spawnParticle(spawnParticle, particleLocation, 5, 0.1, 0.1, 0.1, 0);
        }

        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
            }
        }
    }

    private void executeTornado(Player player, Particle spawnParticle, Particle hitParticle, int damage) {
        player.sendMessage(Component.text("You used Tornado!", NamedTextColor.DARK_PURPLE));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

        for (int height = 0; height < 5; height++) {
            double radius = 2 - (height * 0.3);
            for (int angle = 0; angle < 360; angle += 20) {
                double radians = Math.toRadians(angle);
                double x = Math.cos(radians) * radius;
                double z = Math.sin(radians) * radius;
                Location particleLocation = player.getLocation().clone().add(x, height, z);
                player.getWorld().spawnParticle(spawnParticle, particleLocation, 5, 0.1, 0.1, 0.1, 0);
            }
        }

        for (Entity entity : player.getNearbyEntities(4, 5, 4)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage);
                Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2);
                target.setVelocity(knockback);

                target.getWorld().spawnParticle(hitParticle, target.getLocation(), 1, 0.2, 0.2, 0.2, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            }
        }
    }
}
