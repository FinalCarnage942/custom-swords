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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KatanaAbilities implements Listener {

    private final Map<UUID, Long> quietRushCooldown = new HashMap<>();
    private final Map<UUID, Long> airSlashCooldown = new HashMap<>();
    private final FileConfiguration config;

    public KatanaAbilities(FileConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerUseCutlass(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String cutlassName = ChatColor.translateAlternateColorCodes('&', config.getString("swords.katana.name"));
        if (item == null || !cutlassName.equals(item.getItemMeta().getDisplayName())) {
            return;
        }

        boolean isSneaking = player.isSneaking();
        Action action = event.getAction();

        // Sneak + Left-click for Quiet Rush
        if (isSneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            handleQuietRush(player);
        }

        // Sneak + Right-click for Air Slash
        if (isSneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleAirSlash(player);
        }
    }

    private void handleQuietRush(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.katana.cooldowns.quiet_rush", 5000);

        if (isOnCooldown(playerId, quietRushCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, quietRushCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Quiet Rush cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        double damage = config.getDouble("swords.katana.damage.quiet_rush", 10);
        executeQuietRush(player, damage);

        quietRushCooldown.put(playerId, System.currentTimeMillis());
    }

    private void handleAirSlash(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.katana.cooldowns.air_slash", 7000);

        if (isOnCooldown(playerId, airSlashCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, airSlashCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Air Slash cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        double damage = config.getDouble("swords.katana.damage.air_slash", 15);
        executeAirSlash(player, damage);

        airSlashCooldown.put(playerId, System.currentTimeMillis());
    }

    private boolean isOnCooldown(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long currentTime = System.currentTimeMillis();
        return cooldownMap.containsKey(playerId) && (currentTime - cooldownMap.get(playerId)) < cooldownTime;
    }

    private long getCooldownRemaining(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long lastUseTime = cooldownMap.getOrDefault(playerId, 0L);
        return cooldownTime - (System.currentTimeMillis() - lastUseTime);
    }

    private void executeQuietRush(Player player, double damage) {
        player.sendMessage(Component.text("You used Quiet Rush!", NamedTextColor.RED));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);

        Vector direction = player.getLocation().getDirection().normalize().multiply(1.5);
        Location startLocation = player.getLocation().clone();

        new BukkitRunnable() {
            int steps = 0;

            @Override
            public void run() {
                if (steps >= 10) { // Limit the dash to 10 steps (max distance)
                    cancel();
                    return;
                }

                Location nextLocation = startLocation.clone().add(direction.clone().multiply(steps));
                steps++;

                // Stop if hitting a solid block
                if (nextLocation.getBlock().getType().isSolid()) {
                    cancel();
                    return;
                }

                // Teleport the player
                player.teleport(nextLocation);

                // Create dash effect
                player.getWorld().spawnParticle(Particle.CLOUD, nextLocation, 10, 0.2, 0.2, 0.2, 0.1);

                // Damage entities in range
                for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(damage);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("BloxFruits"), 0L, 1L);
    }


    private void executeAirSlash(Player player, double damage) {
        player.sendMessage(Component.text("You used Air Slash!", NamedTextColor.AQUA));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location startLocation = player.getEyeLocation().add(direction.clone().multiply(1.5));

        for (int i = 0; i < 10; i++) {
            final int step = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location particleLocation = startLocation.clone().add(direction.clone().multiply(step * 0.5));
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 10, 0.1, 0.1, 0.1, 0.1);

                    // Damage entities in range
                    for (Entity entity : particleLocation.getWorld().getNearbyEntities(particleLocation, 1.5, 1.5, 1.5)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            target.damage(damage);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("BloxFruits"), i * 2L);
        }
    }
}
