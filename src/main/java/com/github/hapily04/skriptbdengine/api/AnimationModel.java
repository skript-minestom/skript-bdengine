package com.github.hapily04.skriptbdengine.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.hapily04.skriptbdengine.api.BdEngineModelConverter.GSON;

public class AnimationModel extends StationaryEntity {

    private static final ExecutorService ANIMATION_EXECUTOR = Executors.newCachedThreadPool();

    public boolean isDestroyed = false;
    public List<List<Entity>> groups = new ArrayList<>();
    private final Entity[] entities;
    private JSON.Data data;
    private RunningAnimation runningAnimation;
    boolean initialized = false;

    public AnimationModel(JSON.Data data) {
        super(EntityType.MARKER);
        this.data = data;
        List<Entity> entities = new ArrayList<>();
        if (data.nbt != null) {
            for (String nbt : data.nbt) {
                if (nbt == null || nbt.isEmpty()) {
                    continue;
                }
                JsonElement jsonElement = GSON.fromJson(nbt, JsonElement.class);
                JsonObject obj = jsonElement.getAsJsonObject();
                JsonArray passengers = obj.getAsJsonArray("Passengers");
                if (passengers == null) continue;
                for (JsonElement e : passengers) {
                    entities.add(createEntity(e.getAsJsonObject()));
                }
            }
        }
        this.entities = entities.toArray(new Entity[0]);
        groups.add(entities);
        initialized = true;
    }

    @Override
    public @NonNull CompletableFuture<Void> setInstance(@NonNull Instance instance, @NonNull Pos spawnPosition) {
        int entitySize = entities.length;
        CompletableFuture<?>[] futures = new CompletableFuture[entitySize+1];
        futures[0] = super.setInstance(instance, spawnPosition);
        for (int i = 1; i < entitySize+1; i++) {
            futures[i] = entities[i-1].setInstance(instance, spawnPosition);
        }
        return CompletableFuture.allOf(futures).whenComplete((unused, throwable) -> {
            if (throwable != null) return;
            for (Entity e : entities) {
                addPassenger(e);
            }
        });
    }

    @SuppressWarnings("MagicConstant")
    @Override
    public @NonNull CompletableFuture<Void> teleport(@NonNull Pos position, @NonNull Vec velocity, long @Nullable [] chunks,
                                                     int flags, boolean shouldConfirm) {
        int entitySize = entities.length;
        CompletableFuture<?>[] futures = new CompletableFuture[entitySize+1];
        futures[0] = super.teleport(position, velocity, chunks, flags, shouldConfirm);
        for (int i = 1; i < entitySize+1; i++) {
            futures[i] = entities[i-1].teleport(position, velocity, chunks, flags, shouldConfirm);
        }
        return CompletableFuture.allOf(futures);
    }

    @Override
    protected void remove(boolean permanent) {
        isDestroyed = true;
        super.remove(permanent);
        if (!initialized) return;
        for (Entity e : entities) {
            e.remove();
        }
    }

    @Override
    public void setVelocity(@NonNull Vec velocity) {
        super.setVelocity(velocity);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setVelocity(velocity);
        }
    }

    @Override
    public void setAutoViewable(boolean autoViewable) {
        super.setAutoViewable(autoViewable);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setAutoViewable(autoViewable);
        }
    }

    @Override
    public void setAutoViewEntities(boolean autoViewer) {
        super.setAutoViewEntities(autoViewer);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setAutoViewable(autoViewer);
        }
    }

    @Override
    public void setGlowing(boolean glowing) {
        super.setGlowing(glowing);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setGlowing(glowing);
        }
    }

    @Override
    public void setNoGravity(boolean noGravity) {
        super.setNoGravity(noGravity);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setNoGravity(noGravity);
        }
    }

    @Override
    public void setHasPhysics(boolean hasPhysics) {
        super.setHasPhysics(hasPhysics);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setHasPhysics(hasPhysics);
        }
    }

    @Override
    public void setView(float yaw, float pitch, float headRotation) {
        super.setView(yaw, pitch, headRotation);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setView(yaw, pitch, headRotation);
        }
    }

    @Override
    public void setInvisible(boolean invisible) {
        super.setInvisible(invisible);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setInvisible(invisible);
        }
    }

    @Override
    public void setAerodynamics(@NonNull Aerodynamics aerodynamics) {
        super.setAerodynamics(aerodynamics);
        if (!initialized) return;
        for (Entity e : entities) {
            e.setAerodynamics(aerodynamics);
        }
    }

    @Override
    public boolean addViewer(@NonNull Player player) {
        boolean success = super.addViewer(player);
        for (Entity e : entities) {
            e.addViewer(player);
        }
        return success;
    }

    @Override
    public boolean removeViewer(@NonNull Player player) {
        boolean success = super.removeViewer(player);
        for (Entity e : entities) {
            e.removeViewer(player);
        }
        return success;
    }

    @Override
    public synchronized void switchEntityType(@NonNull EntityType entityType) {}

    private Entity createEntity(JsonObject json) {
        boolean isBlock = json.get("id").getAsString().equals("minecraft:block_display");
        boolean isItem = json.get("id").getAsString().equals("minecraft:item_display");

        Entity entity = new StationaryEntity(isItem ? EntityType.ITEM_DISPLAY : (isBlock ? EntityType.BLOCK_DISPLAY : EntityType.TEXT_DISPLAY));

        Matrix4f matrix = new Matrix4f();
        JsonArray m = json.get("transformation").getAsJsonArray();
        for (int i = 0; i < 16; i++) matrix.set(i / 4, i % 4, m.get(i).getAsFloat());
        matrix.transpose();

        Vector3f transform = new Vector3f();
        matrix.getTranslation(transform);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Quaternionf rotation = new Quaternionf();
        matrix.getUnnormalizedRotation(rotation);

        float[] leftRotation = new float[]{rotation.x, rotation.y, rotation.z, rotation.w};
        Vec translation = new Vec(transform.x, transform.y, transform.z);
        Vec vecScale = new Vec(scale.x, scale.y, scale.z);

        if (isBlock) {
            BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();
            JsonObject block = json.get("block_state").getAsJsonObject();
            Block blockState = buildBlock(block);
            meta.setBlockState(blockState);
            meta.setLeftRotation(leftRotation);
            meta.setTranslation(translation);
            meta.setScale(vecScale);
        } else if (isItem) {
            ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
            Material material = Material.fromKey(json.get("item").getAsJsonObject().get("id").getAsString());
            if (material == null) material = Material.AIR;
            ItemStack itemStack = ItemStack.of(material);
            if (material == Material.PLAYER_HEAD) {
                String textures = json.get("item").getAsJsonObject()
                    .get("components").getAsJsonObject()
                    .get("minecraft:profile").getAsJsonObject()
                    .get("properties").getAsJsonArray()
                    .get(0).getAsJsonObject().get("value").getAsString();
                ResolvableProfile profile = new ResolvableProfile(new PlayerSkin(textures, ""));
                itemStack = itemStack.with(DataComponents.PROFILE, profile);
            }
            meta.setItemStack(itemStack);
            meta.setLeftRotation(leftRotation);
            meta.setTranslation(translation);
            meta.setScale(vecScale);
        } else {
            // Text display
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setLeftRotation(leftRotation);
            meta.setTranslation(translation);
            meta.setScale(vecScale);

            if (json.has("text")) {
                JsonElement textElement = json.get("text");
                Component result;

                if (textElement.isJsonPrimitive()) {
                    String raw = textElement.getAsString();
                    String trimmed = raw.trim();
                    if (trimmed.startsWith("[")) {
                        // Stored as a raw JSON array string: parse each element and merge
                        JsonArray array = GSON.fromJson(trimmed, JsonArray.class);
                        Component combined = Component.empty();
                        for (JsonElement el : array) {
                            Component part = GsonComponentSerializer.gson().deserialize(el.toString());
                            combined = combined.append(part);
                        }
                        result = combined;
                    } else {
                        result = GsonComponentSerializer.gson().deserialize(raw);
                    }
                } else if (textElement.isJsonArray()) {
                    JsonArray array = textElement.getAsJsonArray();
                    Component combined = Component.empty();
                    for (JsonElement el : array) {
                        Component part = GsonComponentSerializer.gson().deserialize(el.toString());
                        combined = combined.append(part);
                    }
                    result = combined;
                } else {
                    String rawJson = textElement.toString();
                    result = GsonComponentSerializer.gson().deserialize(rawJson);
                }

                meta.setText(result);
            }

            if (json.has("background")) meta.setBackgroundColor(json.get("background").getAsInt());

            if (json.has("text_opacity")) meta.setTextOpacity((byte) json.get("text_opacity").getAsInt());

            if (json.has("alignment")) {
                String alignmentName = json.get("alignment").getAsString();
                if (alignmentName != null) {
                    try {
                        TextDisplayMeta.Alignment alignment = TextDisplayMeta.Alignment.valueOf(alignmentName.toUpperCase(Locale.ROOT));
                        meta.setAlignment(alignment);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            if (json.has("line_width")) meta.setLineWidth((int) json.get("line_width").getAsFloat());

            if (json.has("default_background")) meta.setUseDefaultBackground(json.get("default_background").getAsBoolean());
        }

        return entity;
    }

    private Block buildBlock(JsonObject block) {
        JsonObject propertiesObject = block.get("Properties").getAsJsonObject();
        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
            properties.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Block.fromKey(block.get("Name").getAsString()).withProperties(properties);
    }

    private JSON.AnimationJson getAnimation(String name) {
        for (JSON.AnimationJson anim : data.animations) {
            if (anim.name.equals(name)) return anim;
        }
        return null;
    }

    public AnimationModel createCopy() {
        return new AnimationModel(data);
    }

    public String getCurrentAnimation() {
        if (runningAnimation == null) return null;
        return runningAnimation.animationId;
    }

    public RunningAnimation animation(String animation) {
        if (isDestroyed) throw new Error("This model is destroyed");
        JSON.AnimationJson anim = getAnimation(animation);
        if (anim == null) throw new Error("There's no animation");
        if (runningAnimation == null) runningAnimation = new RunningAnimation(anim);
        else runningAnimation.stop().animation(animation);
        return runningAnimation;
    }

    public RunningAnimation play(String animation) {
        return animation(animation).play(false);
    }

    public RunningAnimation play(String animation, boolean loop) {
        return animation(animation).play(loop);
    }

    public void clearAnimation() {
        if (runningAnimation == null) return;
        runningAnimation.stop().animation = null;
    }

    static class JSON {
        public static class Commands {
            public float[] transformation;
            public String head;
        }

        public static class Object {
            public String tag;
            public Commands commands;
        }

        public static class KeyFrame {
            public List<Object> objects;
        }

        public static class AnimationJson {
            public String name;
            public List<KeyFrame> keyframes;
        }

        public static class Data {
            public List<String> nbt;
            public List<AnimationJson> animations;
        }
    }

    public class RunningAnimation {
        private boolean isStopped = false;
        private boolean loop = false;
        private final List<Runnable> onEnd = new ArrayList<>();
        private JSON.AnimationJson animation;
        private String animationId;

        public RunningAnimation(JSON.AnimationJson animation) {
            this.animation = animation;
        }

        public RunningAnimation(String animationName) {
            this.animation = getAnimation(animationName);
        }

        public void update() {
            if (isStopped) return;
            AtomicInteger frameId = new AtomicInteger();
            MinecraftServer.getSchedulerManager().submitTask(() -> {
                if (isStopped) return TaskSchedule.stop();
                List<JSON.KeyFrame> keyFrames = animation.keyframes;
                if (frameId.get() >= keyFrames.size()) {
                    if (loop) frameId.set(0);
                    else return TaskSchedule.stop();
                }
                JSON.KeyFrame kf = keyFrames.get(frameId.getAndIncrement());
                for (JSON.Object obj : kf.objects) {
                    int id = Integer.parseInt(obj.tag.replaceAll("\\D", ""));
                    Entity entity = groups.getFirst().get(id);

                    JSON.Commands commands = obj.commands;
                    if (commands != null) {
                        if (commands.head != null && !commands.head.isEmpty() && entity.getEntityType() == EntityType.ITEM_DISPLAY) {
                            ResolvableProfile profile = new ResolvableProfile(new PlayerSkin(commands.head, ""));
                            ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
                            meta.setItemStack(meta.getItemStack().with(DataComponents.PROFILE, profile));
                        }

                        if (commands.transformation != null) {
                            Matrix4f matrix = new Matrix4f();
                            matrix.set(commands.transformation);
                            matrix.transpose();

                            Vector3f transform = new Vector3f();
                            matrix.getTranslation(transform);

                            Vector3f scale = new Vector3f();
                            matrix.getScale(scale);

                            Quaternionf rotation = new Quaternionf();
                            matrix.getUnnormalizedRotation(rotation);

                            float[] leftRotation = {rotation.x, rotation.y, rotation.z, rotation.w};
                            Vec translation = new Vec(transform.x, transform.y, transform.z);
                            Vec scaleVec = new Vec(scale.x, scale.y, scale.z);
                            EntityMeta entityMeta = entity.getEntityMeta();
                            entityMeta.setNotifyAboutChanges(false);
                            if (entity.getEntityType() == EntityType.BLOCK_DISPLAY) {
                                BlockDisplayMeta meta = (BlockDisplayMeta) entityMeta;
                                meta.setLeftRotation(leftRotation);
                                meta.setTranslation(translation);
                                meta.setScale(scaleVec);
                                meta.setTransformationInterpolationStartDelta(0);
                                meta.setTransformationInterpolationDuration(2);
                                meta.setPosRotInterpolationDuration(2);
                            } else if (entity.getEntityType() == EntityType.ITEM_DISPLAY) {
                                ItemDisplayMeta meta = (ItemDisplayMeta) entityMeta;
                                meta.setLeftRotation(leftRotation);
                                meta.setTranslation(translation);
                                meta.setScale(scaleVec);
                                meta.setTransformationInterpolationStartDelta(0);
                                meta.setTransformationInterpolationDuration(2);
                                meta.setPosRotInterpolationDuration(2);
                            } else if (entity.getEntityType() == EntityType.TEXT_DISPLAY) {
                                TextDisplayMeta meta = (TextDisplayMeta) entityMeta;
                                meta.setLeftRotation(leftRotation);
                                meta.setTranslation(translation);
                                meta.setScale(scaleVec);
                                meta.setTransformationInterpolationStartDelta(0);
                                meta.setTransformationInterpolationDuration(2);
                                meta.setPosRotInterpolationDuration(2);
                            }
                            entityMeta.setNotifyAboutChanges(true);
                        }
                    }
                }
                return TaskSchedule.tick(2);
            }, ExecutionType.TICK_END);
        }

        public void runOnEnd() {
            for (Runnable r : onEnd) r.run();
            onEnd.clear();
        }

        public RunningAnimation onEnd(Runnable r) {
            onEnd.add(r);
            return this;
        }

        public RunningAnimation animation(String animation) {
            if (isDestroyed) throw new Error("This model is destroyed");
            this.animation = getAnimation(animation);
            if (this.animation == null) throw new Error("There's no animation");
            animationId = animation;
            return this;
        }

        public RunningAnimation play() {
            return play(false);
        }

        public RunningAnimation play(boolean loop) {
            this.isStopped = false;
            this.loop = loop;
            update();
            return this;
        }

        public RunningAnimation stop() {
            this.isStopped = true;
            return this;
        }
    }
}
