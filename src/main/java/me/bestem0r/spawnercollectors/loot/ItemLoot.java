package me.bestem0r.spawnercollectors.loot;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class ItemLoot {

    private final Material material;

    private final double probability;
    private final int min;
    private final int max;

    public ItemLoot(Material material, double probability, int min, int max) {
        Bukkit.getLogger().info(material + " | " + probability);
        this.material = material;
        this.probability = probability;
        this.min = min;
        this.max = max;
    }

    public Optional<ItemStack> getRandomLoot() {
        if (ThreadLocalRandom.current().nextDouble() > probability) { return Optional.empty(); }

        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        return Optional.of(new ItemStack(material, amount));
    }
}
