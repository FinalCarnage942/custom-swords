package me.Carnage.bloxFruits;

import net.kyori.adventure.text.Component; // Used for creating text components
import net.kyori.adventure.text.format.NamedTextColor; // Used for text coloring
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration; // Access to the plugin's config file
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity; // Represents any entity in the world
import org.bukkit.entity.LivingEntity; // Represents entities with health
import org.bukkit.entity.Player; // Represents a player
import org.bukkit.event.EventHandler; // Marks event methods
import org.bukkit.event.Listener; // Allows listening to events
import org.bukkit.event.block.Action; // Represents player actions (e.g., left/right click)
import org.bukkit.event.player.PlayerInteractEvent; // Event for player interactions
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable; // Used for scheduling tasks
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector; // Represents 3D vectors (e.g., direction)

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SharkSawAbilities implements Listener {

    private final FileConfiguration config; // Holds the plugin's configuration
    private final Map<UUID, Long> consecutiveSlashesCooldown = new HashMap<>(); // Tracks cooldown for Consecutive Slashes
    private final Map<UUID, Long> executionCooldown = new HashMap<>(); // Tracks cooldown for Execution

    public SharkSawAbilities(FileConfiguration config) {
        this.config = config; // Initializes config for the class
    }

    @EventHandler
    public void onPlayerUseSharkSaw(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if the item matches Shark Saw's configuration
        String sharkSawName = ChatColor.translateAlternateColorCodes('&', config.getString("swords.shark_saw.name"));
        if (item == null || !sharkSawName.equals(item.getItemMeta().getDisplayName())) {
            return; // Exit if the player isn't holding Shark Saw
        }

        boolean isSneaking = player.isSneaking(); // Check if the player is sneaking
        Action action = event.getAction(); // Get the action type (e.g., left/right click)

        // Sneak + Left-click to use Consecutive Slashes
        if (isSneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            handleConsecutiveSlashes(player);
        }

        // Sneak + Right-click to use Execution
        if (isSneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleExecution(player);
        }
    }

    private void handleConsecutiveSlashes(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.shark_saw.cooldowns.consecutive_slashes", 8000); // Get cooldown from config

        // Check if the ability is on cooldown
        if (isOnCooldown(playerId, consecutiveSlashesCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, consecutiveSlashesCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Consecutive Slashes cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        double damage = config.getDouble("swords.shark_saw.damage.consecutive_slashes", 15); // Get damage from config
        executeConsecutiveSlashes(player, damage);

        // Update cooldown tracker
        consecutiveSlashesCooldown.put(playerId, System.currentTimeMillis());
    }

    private void handleExecution(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldown = config.getLong("swords.shark_saw.cooldowns.execution", 12000); // Get cooldown from config

        // Check if the ability is on cooldown
        if (isOnCooldown(playerId, executionCooldown, cooldown)) {
            long remaining = getCooldownRemaining(playerId, executionCooldown, cooldown) / 1000;
            player.sendActionBar(Component.text("Execution cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        double damage = config.getDouble("swords.shark_saw.damage.execution", 25); // Get damage from config
        executeExecution(player, damage);

        // Update cooldown tracker
        executionCooldown.put(playerId, System.currentTimeMillis());
    }

    private boolean isOnCooldown(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long currentTime = System.currentTimeMillis();
        return cooldownMap.containsKey(playerId) && (currentTime - cooldownMap.get(playerId)) < cooldownTime;
    }

    private long getCooldownRemaining(UUID playerId, Map<UUID, Long> cooldownMap, long cooldownTime) {
        long lastUseTime = cooldownMap.getOrDefault(playerId, 0L);
        return cooldownTime - (System.currentTimeMillis() - lastUseTime);
    }

    private void executeConsecutiveSlashes(Player player, double damage) {
        player.sendMessage(Component.text("You used Consecutive Slashes!", NamedTextColor.DARK_RED));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1, 1); // Play activation sound

        // Perform three slashes (two diagonals + one vertical)
        String[] slashDirections = {"diagonal_down", "diagonal_up", "vertical"};

        // Use a BukkitRunnable to execute slashes sequentially
        for (int i = 0; i < slashDirections.length; i++) {
            final int index = i; // To use in the lambda expression
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnSlash(player, damage, slashDirections[index]);
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("BloxFruits"), i * 10L); // Delays between slashes
        }
    }

    private void spawnSlash(Player player, double damage, String direction) {
        // Determine the direction and spawn slashes with particles
        Location location = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2.5));

        if ("diagonal_down".equals(direction)) {
            player.getWorld().spawnParticle(Particle.SQUID_INK, location, 10, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, location, 200, 0.5, -0.5, 0.5, 0.1);
        } else if ("diagonal_up".equals(direction)) {
            player.getWorld().spawnParticle(Particle.SQUID_INK, location, 10, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, location, 200, 0.5, -0.5, 0.5, 0.1);
        } else if ("vertical".equals(direction)) {
            player.getWorld().spawnParticle(Particle.SQUID_INK, location, 10, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, location, 200, 0.5, -0.5, 0.5, 0.1);

        }

        // Damage entities in range of the slash
        for (Entity entity : location.getWorld().getNearbyEntities(location, 5, 2, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(damage);
            }
        }
    }

    private void executeExecution(Player player, double damage) {
        // Get the item in the player's main hand (the mace)
        ItemStack mace = player.getInventory().getItemInMainHand();

        // Check if the player is holding a valid item
        if (mace == null || !mace.hasItemMeta() || mace.getItemMeta().getDisplayName().isEmpty()) {
            player.sendMessage(Component.text("You must be holding the Shark Saw to use Execution!", NamedTextColor.RED));
            return;
        }

        // Temporarily remove the mace from the player's hand
        player.getInventory().setItemInMainHand(null);

        // Notify the player
        player.sendMessage(Component.text("You used Execution! The Shark Saw spins around you!", NamedTextColor.RED));

        // Play the start sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        // Spawn an armor stand to display the mace
        Location center = player.getLocation();
        ArmorStand armorStand = center.getWorld().spawn(center, ArmorStand.class, as -> {
            as.setVisible(false); // Hide the armor stand
            as.setSmall(true); // Make it a small armor stand
            as.setGravity(false); // Disable gravity
            as.setMarker(true); // Prevent collisions
            as.getEquipment().setHelmet(mace); // Display the mace on the armor stand
            as.setHeadPose(new EulerAngle(Math.toRadians(90), 0, 0)); // Rotate the mace to be parallel to the ground
        });

        // Rotation variables
        double radius = 2.0;
        int totalSteps = 20;
        int stepDelay = 3; // Time between rotations in ticks
        double angleIncrement = 360.0 / totalSteps;

        new BukkitRunnable() {
            int step = 0; // Tracks the current rotation step
            double currentRotation = 0.0; // Tracks the mace's rotation around its axis

            @Override
            public void run() {
                if (step >= totalSteps) { // End of the rotation
                    // Perform the final attack
                    performFinalHorizontalSlash(player, damage);

                    // Remove the armor stand
                    armorStand.remove();

                    // Return the mace to the player's hand
                    player.getInventory().setItemInMainHand(mace);

                    // Notify the player
                    player.sendMessage(Component.text("The Shark Saw returns to your hand!", NamedTextColor.GREEN));
                    cancel();
                    return;
                }

                // Calculate the new position of the mace (rotating around the player)
                double angle = Math.toRadians(angleIncrement * step);
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                double y = center.getY() + 1.5; // Slightly above the player's head

                Location maceLocation = new Location(center.getWorld(), x, y, z);

                // Move the armor stand to the calculated position
                armorStand.teleport(maceLocation);

                // Update the mace's rotation (spin the mace on its axis)
                currentRotation += Math.toRadians(18); // Rotate 18 degrees per step
                armorStand.setHeadPose(new EulerAngle(Math.toRadians(90), currentRotation, 0)); // Maintain parallel alignment while spinning

                // Spawn particles to enhance visuals
                player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, maceLocation, 250, 0.1, 0.1, 0.1, 0.1);
                player.getWorld().spawnParticle(Particle.SQUID_INK, maceLocation, 50, 0.1, 0.1, 0.1, 0.1);

                // Damage nearby entities during the rotation
                for (Entity entity : maceLocation.getWorld().getNearbyEntities(maceLocation, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(damage / totalSteps); // Spread the total damage across steps
                    }
                }

                step++; // Increment the step for the next rotation
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("BloxFruits"), 0L, stepDelay);
    }

    /**
     * Performs the final slash after the rotation is complete.
     */
    private void performFinalHorizontalSlash(Player player, double damage) {
        // Play finishing sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 1);

        // Spawn finishing particles
        player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation(), 5, 1, 0.5, 1, 0.1);

        // Apply knockback and damage to enemies caught in the attack
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 2, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Damage the target
                target.damage(damage);

                // Apply horizontal knockback
                Vector knockbackVector = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize()
                        .multiply(1.2); // Adjustable knockback strength
                target.setVelocity(knockbackVector);
            }
        }
    }


}
