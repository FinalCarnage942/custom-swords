package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TripleKatanaAbilities implements Listener {

    private final FileConfiguration config;
    private final Map<UUID, ArmorStand> mouthSwordMap = new HashMap<>();
    private final Map<UUID, BukkitRunnable> trackingTasks = new HashMap<>();
    private final Map<UUID, Long> airSlashesBarrageCooldown = new HashMap<>();
    private final Map<UUID, Long> violentRushCooldown = new HashMap<>();

    public TripleKatanaAbilities(FileConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerUseTripleKatana(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String tripleKatanaName = ChatColor.translateAlternateColorCodes('&', config.getString("swords.triple_katana.name"));
        if (item == null || !tripleKatanaName.equals(item.getItemMeta().getDisplayName())) {
            return;
        }

        Action action = event.getAction();
        boolean isSneaking = player.isSneaking();

        if (isSneaking && action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            handleAirSlashesBarrage(player);
        } else if (isSneaking && action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            handleViolentRush(player);
        }
    }

    @EventHandler
    public void onPlayerHoldTripleKatana(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        String itemType = config.getString("swords.triple_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null || newItem == null || newItem.getType() != material || newItem.getItemMeta() == null) {
            if (offHandItem != null && offHandItem.getType() == material) {
                player.getInventory().setItemInOffHand(null);
            }
            removeMouthSword(player);
            return;
        }

        String itemName = newItem.getItemMeta().getDisplayName();
        if (itemName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.triple_katana.name")))) {
            equipTripleKatanas(player);
        }
    }

    @EventHandler
    public void onPlayerDropTripleKatana(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        String itemType = config.getString("swords.triple_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null || droppedItem.getType() != material || droppedItem.getItemMeta() == null) {
            return;
        }

        String itemName = droppedItem.getItemMeta().getDisplayName();
        if (itemName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.triple_katana.name")))) {
            player.getInventory().setItemInOffHand(null);
            removeMouthSword(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        String itemType = config.getString("swords.triple_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material != null && mainHandItem != null && mainHandItem.getType() == material) {
            String mainHandName = mainHandItem.getItemMeta().getDisplayName();
            if (mainHandName.equals(ChatColor.translateAlternateColorCodes('&', config.getString("swords.triple_katana.name")))) {
                if (event.getSlot() == EquipmentSlot.OFF_HAND.ordinal()) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot remove the off-hand katana while holding the Triple Katana.")
                            .color(NamedTextColor.RED));
                }
            }
        }
    }

    private void equipTripleKatanas(Player player) {
        String itemType = config.getString("swords.triple_katana.item");
        Material material = Material.matchMaterial(itemType);

        if (material == null) {
            player.sendMessage(Component.text("Invalid item type in the configuration for Triple Katana!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Equip the off-hand katana
        ItemStack offHand = createKatanaItem(material, config.getString("swords.triple_katana.name"));
        player.getInventory().setItemInOffHand(offHand);

        // Create and align the mouth katana
        createMouthSword(player, material, config.getString("swords.triple_katana.name"));

        // Notify the player
        player.sendMessage(Component.text("You are now wielding the Triple Katana!")
                .color(NamedTextColor.GREEN));
    }

    private void createMouthSword(Player player, Material material, String name) {
        removeMouthSword(player); // Ensure no existing mouth sword

        ArmorStand mouthSword = player.getWorld().spawn(player.getLocation().add(0, 1.3, 0), ArmorStand.class);
        mouthSword.setInvisible(true);
        mouthSword.setMarker(true);
        mouthSword.setGravity(false);
        mouthSword.setSmall(true);

        ItemStack swordItem = createKatanaItem(material, name);
        mouthSword.getEquipment().setItemInMainHand(swordItem);

        mouthSwordMap.put(player.getUniqueId(), mouthSword);

        // Start tracking the player's movement to keep the sword aligned
        startTrackingMouthSword(player, mouthSword);
    }

    private void removeMouthSword(Player player) {
        UUID playerId = player.getUniqueId();
        ArmorStand mouthSword = mouthSwordMap.remove(playerId);
        if (mouthSword != null) {
            mouthSword.remove();
        }

        BukkitRunnable trackingTask = trackingTasks.remove(playerId);
        if (trackingTask != null) {
            trackingTask.cancel();
        }
    }

    private void startTrackingMouthSword(Player player, ArmorStand mouthSword) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!mouthSword.isValid() || !player.isOnline() || !player.isValid()) {
                    cancel();
                    removeMouthSword(player);
                    return;
                }

                Location mouthLocation = player.getLocation().add(player.getLocation().getDirection().multiply(0.4));
                mouthLocation.add(0, 1.3, 0); // Position above ground level
                mouthSword.teleport(mouthLocation);

                // Rotate the sword sideways
                mouthSword.setRotation(player.getLocation().getYaw(), 0);
            }
        };

        trackingTask.runTaskTimer(Bukkit.getPluginManager().getPlugin("BloxFruits"), 0L, 1L); // Update every tick
        trackingTasks.put(playerId, trackingTask);
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
    private void handleAirSlashesBarrage(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.triple_katana.cooldowns.air_slashes_barrage", 7000);

        if (isOnCooldown(playerId, airSlashesBarrageCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, airSlashesBarrageCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Air Slashes Barrage cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        double damage = config.getDouble("swords.triple_katana.damage.air_slashes_barrage", 12);
        executeAirSlashesBarrage(player, damage);

        airSlashesBarrageCooldown.put(playerId, System.currentTimeMillis());
    }

    private void handleViolentRush(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.triple_katana.cooldowns.violent_rush", 10000);

        if (isOnCooldown(playerId, violentRushCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, violentRushCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Violent Rush cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        double damage = config.getDouble("swords.triple_katana.damage.violent_rush", 20);
        executeViolentRush(player, damage);

        violentRushCooldown.put(playerId, System.currentTimeMillis());
    }

    private boolean isOnCooldown(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long currentTime = System.currentTimeMillis();
        return cooldownMap.containsKey(playerId) && (currentTime - cooldownMap.get(playerId)) < cooldownTime;
    }

    private long getCooldownRemaining(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long lastUseTime = cooldownMap.getOrDefault(playerId, 0L);
        return cooldownTime - (System.currentTimeMillis() - lastUseTime);
    }

    private void executeAirSlashesBarrage(Player player, double damage) {
        player.sendMessage(Component.text("You used Air Slashes Barrage!", NamedTextColor.AQUA));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location startLocation = player.getEyeLocation().add(direction.clone().multiply(1.5));

        String[] orientations = {"horizontal", "vertical", "horizontal"};

        for (int i = 0; i < orientations.length; i++) {
            final int index = i; // For use in lambda
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnAirSlash(player, startLocation, direction, damage, orientations[index]);
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("BloxFruits"), i * 10L); // Delay each by 10 ticks
        }
    }

    private void spawnAirSlash(Player player, Location startLocation, Vector direction, double damage, String orientation) {
        // Set maximum range
        int maxRange = 10; // Define the maximum steps or range for the air slash

        for (int step = 0; step < maxRange; step++) {
            Location current = startLocation.clone().add(direction.clone().multiply(step * 0.5));

            // Create the particle effect based on orientation
            if ("horizontal".equals(orientation)) {
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, current, 10, 0.5, 0, 0.5, 0.1);
            } else if ("vertical".equals(orientation)) {
                // For vertical, apply the same range and effect
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, current, 10, 0.5, 0.5, 0.5, 0.1);
            }

            // Damage entities in the slash's path
            for (Entity entity : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    target.damage(damage);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
                    target.getWorld().spawnParticle(Particle.RAIN, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }


    private void executeViolentRush(Player player, double damage) {
        UUID playerId = player.getUniqueId();

        // Temporarily remove the sword
        ItemStack sword = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(null);

        player.sendMessage(Component.text("You used Violent Rush!", NamedTextColor.RED));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);

        Vector dashVector = player.getLocation().getDirection().normalize().multiply(1.5);
        Location startLocation = player.getLocation();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                Location current = startLocation.clone().add(dashVector.clone().multiply(step));

                // Stop if we've reached the maximum steps or if the current block is solid
                if (step >= 10 || current.getBlock().getType().isSolid()) {
                    player.getInventory().setItemInMainHand(sword); // Return the sword
                    cancel();
                    return;
                }

                // Teleport the player to the current location
                player.teleport(current);

                // Create the dash effect
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, current, 10, 0.1, 0.1, 0.1, 0.1);

                // Damage nearby entities
                for (Entity entity : current.getWorld().getNearbyEntities(current, 2, 2, 2)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(damage);
                    }
                }

                step++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("BloxFruits"), 0L, 2L);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        // Triple Katana item name and material
        String tripleKatanaName = ChatColor.translateAlternateColorCodes('&', config.getString("swords.triple_katana.name"));
        Material tripleKatanaMaterial = Material.matchMaterial(config.getString("swords.triple_katana.item"));

        // Check if the dropped item matches the Triple Katana
        if (tripleKatanaMaterial != null && droppedItem.getType() == tripleKatanaMaterial) {
            String itemName = droppedItem.getItemMeta() != null ? droppedItem.getItemMeta().getDisplayName() : null;
            if (tripleKatanaName.equals(itemName)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot drop the Triple Katana!").color(NamedTextColor.RED));
                return;
            }
        }

        // Check if the dropped item matches the off-hand or mouth katana
        if (isGeneratedKatana(droppedItem)) {
            event.setCancelled(true);
        }
    }

    private boolean isGeneratedKatana(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;

        // Match against the Triple Katana's generated katana
        String katanaName = ChatColor.translateAlternateColorCodes('&', config.getString("swords.triple_katana.name"));
        Material material = Material.matchMaterial(config.getString("swords.triple_katana.item"));

        return material != null && item.getType() == material && katanaName.equals(item.getItemMeta().getDisplayName());
    }

}
