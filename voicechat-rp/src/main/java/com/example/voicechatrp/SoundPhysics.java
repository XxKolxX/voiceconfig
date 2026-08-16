package com.example.voicechatrp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SoundPhysics {

    // Returns a multiplier from 0.0 (silent) to 1.0 (full volume)
    // Note: This method MUST be called synchronously on the main thread!
    public static double calculateAttenuationSync(Location source, Location listener, double maxDistance) {
        if (source.getWorld() != listener.getWorld()) {
            return 0.0;
        }

        double distance = source.distance(listener);
        if (distance > maxDistance) {
            return 0.0;
        }

        double multiplier = 1.0;
        Vector direction = listener.toVector().subtract(source.toVector());

        if (direction.lengthSquared() < 0.1) {
             return 1.0;
        }

        direction.normalize();

        double checkDistance = 0.5;
        Location currentLoc = source.clone();
        Set<Block> checkedBlocks = new HashSet<>();

        // Raytrace to find blocks between source and listener
        for (double d = 0; d < distance; d += checkDistance) {
            currentLoc.add(direction.clone().multiply(checkDistance));
            Block block = currentLoc.getBlock();

            // Only apply penalty once per physical block
            if (!checkedBlocks.contains(block)) {
                checkedBlocks.add(block);
                Material type = block.getType();

                if (!type.isAir() && type.isSolid()) {
                    if (type.name().contains("DOOR") || type.name().contains("TRAPDOOR")) {
                        multiplier *= 0.90; // Minor reduction for doors
                    } else if (type.name().contains("GLASS")) {
                        multiplier *= 0.85; // Minor reduction for glass
                    } else {
                        multiplier *= 0.65; // Moderate reduction per solid block (so 1 block won't instantly mute)
                    }
                }

                if (multiplier < 0.01) {
                    return 0.0;
                }
            }
        }

        return multiplier;
    }
}
