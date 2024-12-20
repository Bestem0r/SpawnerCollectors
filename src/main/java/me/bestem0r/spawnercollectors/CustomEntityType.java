package me.bestem0r.spawnercollectors;

import net.bestemor.core.config.ConfigManager;
import org.bukkit.entity.EntityType;

public class CustomEntityType {

    private boolean custom = false;

    private EntityType entityType;
    private String customType;

    public CustomEntityType(EntityType type) {
        if (type == EntityType.CAVE_SPIDER && ConfigManager.getBoolean("cave_spider_is_spider")) {
            entityType = EntityType.SPIDER;
        } else {
            this.entityType = type;
        }
    }

    public CustomEntityType(String type) {
        if (type.equals("MUSHROOM")) {
            type = "MUSHROOM_COW";
        }
        if (type.equals("CAVE_SPIDER") && ConfigManager.getBoolean("cave_spider_is_spider")) {
            type = "SPIDER";
        }
        if (isEntity(type.toUpperCase())) {
            this.entityType = EntityType.valueOf(type.toUpperCase());
        } else {
            this.customType = type;
            this.custom = true;
        }
    }

    private boolean isEntity(String entityTest) {
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().equals(entityTest)) {
                return true;
            }
        }
        return false;
    }

    public String name() {
        return custom ? customType : entityType.name();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public boolean isCustom() {
        return custom;
    }
}
