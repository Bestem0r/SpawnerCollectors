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
        type = type.toUpperCase(Locale.ROOT).replace("MUSHROOM", "MUSHROOM_COW");
        if (isEntity(type)) {
            this.entityType = EntityType.valueOf(type);
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
