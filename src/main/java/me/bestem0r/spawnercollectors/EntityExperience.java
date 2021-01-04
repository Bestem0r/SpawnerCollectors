package me.bestem0r.spawnercollectors;

import org.bukkit.entity.EntityType;

import java.util.concurrent.ThreadLocalRandom;

public enum EntityExperience {
    BEE(1, 3),
    CAT(1, 3),
    CHICKEN(1, 3),
    COD(1, 3),
    SALMON(1, 3),
    PUFFERFISH(1, 3),
    TROPICAL_FISH(1, 3),
    COW(1, 3),
    FOX(1, 3),
    HORSE(1, 3),
    DONKEY(1, 3),
    MULE(1, 3),
    SKELETON_HORSE(1, 3),
    ZOMBIE_HORSE(1, 3),
    LLAMA(1, 3),
    TRADER_LLAMA(1, 3),
    MUSHROOM_COW(1, 3),
    OCELOT(1, 3),
    PANDA(1, 3),
    PARROT(1, 3),
    PIG(1, 3),
    POLAR_BEAR(1, 3),
    RABBIT(1, 3),
    SHEEP(1, 3),
    SQUID(1, 3),
    DOLPHIN(1, 3),
    TURTLE(1, 3),
    WOLF(1, 3),

    STRIDER(1, 2),

    CAVE_SPIDER(5, 5),
    CREEPER(5, 5),
    DROWNED(5, 5),
    ENDERMAN(5, 5),
    GHAST(5, 5),
    HOGLIN(5, 5),
    HUSK(5, 5),
    ILLUSIONER(5, 5),
    PHANTOM(5, 5),
    PIGLIN(5, 5),
    PILLAGER(5, 5),
    SHULKER(5, 5),
    SILVERFISH(5, 5),
    SKELETON(5, 5),
    SPIDER(5, 5),
    STRAY(5, 5),
    VEX(5, 5),
    VINDICATOR(5, 5),
    WITCH(5, 5),
    WITHER_SKELETON(5, 5),
    ZOMBIE(5, 5),
    ZOMBIE_VILLAGER(5, 5),
    ZOGLIN(5, 5),
    ZOMBIEFIED_PIGLIN(5, 5),

    ENDERMITE(3, 3),

    SLIME(1, 4),
    MAGMA_CUBE(1, 4),

    BLAZE(10, 10),
    EVOKER(10, 10),
    ELDER_GUARDIAN(10, 10),
    GUARDIAN(10, 10),

    RAVAGER(50, 50),
    PIGLIN_BRUTE(50, 50)

    ;

    private final int min;
    private final int max;

    EntityExperience(int min, int max) {
        this.min = min;
        this.max = max;

    }

    public int getRandomAmount(int amount) {
        int xp = 0;
        for (int i = 0; i < amount; i++) {
            xp += ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        return xp;
    }
}
