package com.github.hapily04.skriptbdengine.api;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class StationaryEntity extends Entity {

    public StationaryEntity(@NotNull EntityType entityType) {
        super(entityType);
        this.hasPhysics = false;
        this.collidesWithEntities = false;
        setNoGravity(true);
    }

}
