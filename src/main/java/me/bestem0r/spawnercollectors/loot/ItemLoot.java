package me.bestem0r.spawnercollectors.loot;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class ItemLoot {

    private final ItemStack item;

    private final double probability;
    private final int min;
    private final int max;

    public ItemLoot(ItemStack item, double probability, int min, int max) {
        this.item = item;
        this.probability = probability;
        this.min = min;
        this.max = max;
    }

    public Optional<ItemStack> getRandomLoot() {
        if (ThreadLocalRandom.current().nextDouble() > probability) { return Optional.empty(); }

        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        ItemStack item = this.item.clone();
        item.setAmount(amount);
        return Optional.of(item);
    }
}
