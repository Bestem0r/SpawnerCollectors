package me.bestem0r.spawnercollectors;

import org.bukkit.entity.EntityType;

import java.util.Locale;

public class CustomEntityType {

    private boolean custom = false;

    private EntityType entityType;
    private String customType;

    public CustomEntityType(EntityType type) {
        this.entityType = type;
    }

    public CustomEntityType(String type) {
        if (isEntity(type.toUpperCase(Locale.ROOT))) {
            this.entityType = EntityType.valueOf(type.toUpperCase(Locale.ROOT));
        } else {
            this.customType = type.toUpperCase(Locale.ROOT);
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
