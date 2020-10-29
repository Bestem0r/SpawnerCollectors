package me.bestem0r.spawnercollectors;


import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class CollectedSpawner {

    private Instant nextSpawn = Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(2, 41));;

    public int attemptSpawn() {
        if (Instant.now().isAfter(nextSpawn)) {
            this.nextSpawn = Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(10, 41));
            return 4;
        }
        return 0;
    }
}
