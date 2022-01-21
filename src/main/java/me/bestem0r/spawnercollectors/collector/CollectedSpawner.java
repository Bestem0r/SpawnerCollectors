package me.bestem0r.spawnercollectors.collector;


import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class CollectedSpawner {

    private final int amount;
    private final int minTime;
    private final int maxTime;

    private Instant nextSpawn;

    public CollectedSpawner(int amount, int minTime, int maxTime) {
        this.amount = amount;
        this.minTime = minTime;
        this.maxTime = maxTime;

        this.nextSpawn = Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(minTime, maxTime + 1));
    }

    public int attemptSpawn() {
        if (Instant.now().isAfter(nextSpawn)) {
            this.nextSpawn = Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(minTime, maxTime + 1));
            return amount;
        }
        return 0;
    }
}
