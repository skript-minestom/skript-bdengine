package com.github.hapily04.skriptbdengine.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
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
    final Entity[] entities;
    private final Map<String, Entity> entitiesByTag = new HashMap<>();
    private final JSON.Data data;
    private RunningAnimation runningAnimation;
    private boolean initialized = false;
    private final AnimationModelMeta fakeMeta = new AnimationModelMeta(this, new MetadataHolder(_ -> {}));

    public AnimationModel(JSON.Data data) {
        super(EntityType.ITEM_DISPLAY);
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
                if (passengers != null) {
                    for (JsonElement e : passengers) {
                        entities.add(createEntity(e.getAsJsonObject()));
                    }
                } else if (obj.has("id")) {
                    entities.add(createEntity(obj));
                }
            }
        }
        this.entities = entities.toArray(new Entity[0]);
        groups.add(entities);
        initialized = true;
    }

    @Override
    public @NonNull EntityMeta getEntityMeta() {
        return fakeMeta;
    }

    public @Nullable Entity getEntityByTag(String tag) {
        return entitiesByTag.get(tag);
    }

    private void registerEntityTag(String tag, Entity entity) {
        if (tag == null || tag.isEmpty()) return;
        entitiesByTag.put(tag, entity);
    }

    private void registerEntityTags(JsonObject json, Entity entity) {
        if (!json.has("Tags")) return;
        JsonArray tags = json.getAsJsonArray("Tags");
        for (JsonElement tagElement : tags) {
            registerEntityTag(tagElement.getAsString(), entity);
        }
    }

    @Override
    public @NonNull CompletableFuture<Void> setInstance(@NonNull Instance instance, @NonNull Pos spawnPosition) {
        if (isDestroyed) return CompletableFuture.completedFuture(null);
        int entitySize = entities.length;
        CompletableFuture<?>[] futures = new CompletableFuture[entitySize + 1];
        futures[0] = super.setInstance(instance, spawnPosition);
        for (int i = 1; i < entitySize + 1; i++) {
            futures[i] = entities[i - 1].setInstance(instance, spawnPosition);
        }
        return CompletableFuture.allOf(futures).whenComplete((unused, throwable) -> {
            if (throwable != null) return;
            for (Entity e : entities) {
                if (!isCameraEntity(e)) addPassenger(e);
            }
        });
    }

    private boolean isCameraEntity(Entity entity) {
        for (Map.Entry<String, Entity> entry : entitiesByTag.entrySet()) {
            if (entry.getValue() == entity && entry.getKey().endsWith("_camera")) return true;
        }
        return false;
    }

    @Override
    public @NonNull CompletableFuture<Void> teleport(@NonNull Pos position, @NonNull Vec velocity, long [] chunks,
                                                     int flags, boolean shouldConfirm) {
        if (isDestroyed) return CompletableFuture.completedFuture(null);
        int entitySize = entities.length;
        CompletableFuture<?>[] futures = new CompletableFuture[entitySize + 1];
        futures[0] = super.teleport(position, velocity, chunks, flags, shouldConfirm);
        for (int i = 1; i < entitySize + 1; i++) {
            futures[i] = entities[i - 1].teleport(position, velocity, chunks, flags, shouldConfirm);
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

    // todo revert fork commit & update to updateNewViewer
    @Override
    public boolean addViewer(@NonNull Player player) {
        boolean success = super.addViewer(player);
        for (Entity e : entities) {
            e.addViewer(player);
        }
        return success;
    }

    // todo revert fork commit & update to updateNewViewer
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

        JsonArray m = json.get("transformation").getAsJsonArray();
        DisplayMatrixUtil.CachedTransform transform = DisplayMatrixUtil.cacheFromJsonArray(m);

        if (isBlock) {
            BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();
            JsonObject block = json.get("block_state").getAsJsonObject();
            Block blockState = buildBlock(block);
            meta.setBlockState(blockState);
            transform.applyTo(meta);
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
            transform.applyTo(meta);
            if (json.has("item_display")) {
                String displayContext = json.get("item_display").getAsString();
                if (displayContext != null) {
                    try {
                        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.valueOf(displayContext.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            if (json.has("width")) meta.setWidth(json.get("width").getAsFloat());
            if (json.has("height")) meta.setHeight(json.get("height").getAsFloat());
        } else {
            // Text display
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            transform.applyTo(meta);

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

        registerEntityTags(json, entity);
        return entity;
    }

    private static void applyAnimatedTransformation(AbstractDisplayMeta meta) {
        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(2);
        meta.setPosRotInterpolationDuration(2);
    }

    private static boolean keyFramesVisuallyEqual(JSON.KeyFrame a, JSON.KeyFrame b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        List<JSON.Object> aObjects = a.objects;
        List<JSON.Object> bObjects = b.objects;
        if (aObjects == bObjects) return true;
        if (aObjects == null || bObjects == null) return false;
        if (aObjects.size() != bObjects.size()) return false;
        for (int i = 0; i < aObjects.size(); i++) {
            if (!objectsVisuallyEqual(aObjects.get(i), bObjects.get(i))) return false;
        }
        return true;
    }

    private static boolean objectsVisuallyEqual(JSON.Object a, JSON.Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.tag, b.tag) && commandsVisuallyEqual(a.commands, b.commands);
    }

    private static boolean commandsVisuallyEqual(JSON.Commands a, JSON.Commands b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Arrays.equals(a.transformation, b.transformation)
            && Objects.equals(a.head, b.head)
            && Objects.equals(a.text, b.text)
            && Objects.equals(a.background, b.background)
            && Objects.equals(a.textOpacity, b.textOpacity)
            && Objects.equals(a.alignment, b.alignment)
            && Objects.equals(a.lineWidth, b.lineWidth)
            && Objects.equals(a.defaultBackground, b.defaultBackground)
            && Objects.equals(a.x, b.x)
            && Objects.equals(a.y, b.y)
            && Objects.equals(a.z, b.z)
            && Objects.equals(a.yaw, b.yaw)
            && Objects.equals(a.pitch, b.pitch);
    }

    private Block buildBlock(JsonObject block) {
        JsonObject propertiesObject = block.get("Properties").getAsJsonObject();
        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
            properties.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Block.fromKey(block.get("Name").getAsString()).withProperties(properties);
    }

    private void playKeyframeSounds(JSON.KeyFrame keyFrame) {
        if (keyFrame.sounds == null || keyFrame.sounds.isEmpty()) return;

        Instance instance = getInstance();
        if (instance == null) return;

        Pos root = getPosition();
        Collection<Player> players = instance.getPlayers();
        if (players.isEmpty()) return;

        for (JSON.SoundEffect soundEffect : keyFrame.sounds) {
            if (soundEffect.sound == null || soundEffect.sound.isEmpty()) continue;

            String soundKey = soundEffect.sound.contains(":")
                ? soundEffect.sound
                : "minecraft:" + soundEffect.sound;
            Sound.Source source = Sound.Source.BLOCK;
            if (soundEffect.source != null) {
                try {
                    source = Sound.Source.valueOf(soundEffect.source.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {}
            }

            float volume = soundEffect.volume != null ? soundEffect.volume : 1f;
            float pitch = soundEffect.pitch != null ? soundEffect.pitch : 1f;
            Pos at = new Pos(
                root.x() + (soundEffect.x != null ? soundEffect.x : 0d),
                root.y() + (soundEffect.y != null ? soundEffect.y : 0d),
                root.z() + (soundEffect.z != null ? soundEffect.z : 0d)
            );
            Sound sound = Sound.sound(Key.key(soundKey), source, volume, pitch);
            for (Player player : players) {
                player.playSound(sound, at);
            }
        }
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
            public String text;
            public Integer background;
            public Integer textOpacity;
            public String alignment;
            public Float lineWidth;
            public Boolean defaultBackground;
            public Double x;
            public Double y;
            public Double z;
            public Float yaw;
            public Float pitch;
        }

        public static class Object {
            public String tag;
            public Commands commands;
        }

        public static class KeyFrame {
            public List<Object> objects;
            public List<SoundEffect> sounds;
        }

        public static class SoundEffect {
            public String sound;
            public String source;
            public Double x;
            public Double y;
            public Double z;
            public Float volume;
            public Float pitch;
        }

        public static class CameraJson {
            public String tag;
            public String type;
        }

        public static class AnimationJson {
            public String name;
            public List<KeyFrame> keyframes;
        }

        public static class Data {
            public List<String> nbt;
            public List<AnimationJson> animations;
            public CameraJson camera;
        }
    }

    public class RunningAnimation {
        private boolean isStopped = false;
        private boolean loop = false;
        private Task task;
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
            if (isStopped || isDestroyed) return;
            if (task != null) {
                task.cancel();
                task = null;
            }
            AtomicInteger frameId = new AtomicInteger();
            task = MinecraftServer.getSchedulerManager().submitTask(() -> {
                if (isStopped || isDestroyed) return TaskSchedule.stop();
                List<JSON.KeyFrame> keyFrames = animation.keyframes;
                if (frameId.get() >= keyFrames.size()) {
                    if (!loop) return TaskSchedule.stop();
                    int next = 0;
                    if (keyFrames.size() > 1
                        && keyFramesVisuallyEqual(keyFrames.getFirst(), keyFrames.getLast())) {
                        next = 1;
                    }
                    frameId.set(next);
                }
                JSON.KeyFrame kf = keyFrames.get(frameId.getAndIncrement());
                playKeyframeSounds(kf);
                if (kf.objects == null) return TaskSchedule.tick(2);
                for (JSON.Object obj : kf.objects) {
                    Entity entity = entitiesByTag.get(obj.tag);
                    if (entity == null) {
                        int id = Integer.parseInt(obj.tag.replaceAll("\\D", ""));
                        entity = groups.getFirst().get(id);
                    }

                    JSON.Commands commands = obj.commands;
                    if (commands != null) {
                        if (commands.x != null && commands.y != null && commands.z != null) {
                            Pos root = AnimationModel.this.getPosition();
                            float yaw = commands.yaw != null ? commands.yaw : root.yaw();
                            float pitch = commands.pitch != null ? commands.pitch : root.pitch();
                            entity.teleport(new Pos(
                                root.x() + commands.x,
                                root.y() + commands.y,
                                root.z() + commands.z,
                                yaw,
                                pitch
                            ));
                        }

                        if (commands.head != null && !commands.head.isEmpty() && entity.getEntityType() == EntityType.ITEM_DISPLAY) {
                            ResolvableProfile profile = new ResolvableProfile(new PlayerSkin(commands.head, ""));
                            ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
                            meta.setItemStack(meta.getItemStack().with(DataComponents.PROFILE, profile));
                        }

                        if (commands.transformation != null) {
                            EntityMeta entityMeta = entity.getEntityMeta();
                            entityMeta.setNotifyAboutChanges(false);
                            if (entityMeta instanceof AbstractDisplayMeta displayMeta) {
                                DisplayMatrixUtil.cacheFromFlat(commands.transformation).applyTo(displayMeta);
                                applyAnimatedTransformation(displayMeta);
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
            if (task != null) {
                task.cancel();
                task = null;
            }
            return this;
        }
    }
}
