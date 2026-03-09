package com.github.hapily04.skriptbdengine.api;

import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BdEngineModelConverter {

    private static final Pattern ENTITY_SELECTOR_PATTERN = Pattern.compile("@e\\[(.*?)]");
    private static final Pattern TYPE_PATTERN = Pattern.compile("type=([^,]+),");
    private static final Pattern TAG_PATTERN = Pattern.compile("tag=([^,]+),");
    private static final Pattern TRANSFORMATION_PATTERN = Pattern.compile("transformation:\\[(.*?)]");
    // Legacy simple text:'...' pattern (kept for backwards compatibility)
    private static final Pattern LEGACY_TEXT_NBT_PATTERN = Pattern.compile("text:'([^']*)'");
    // text:[ {...} ] component array for text displays
    private static final Pattern TEXT_COMPONENT_PATTERN = Pattern.compile("text:(\\[[^]]*])");
    private static final Pattern BACKGROUND_PATTERN = Pattern.compile("background:([^,}]+)");
    private static final Pattern TEXT_OPACITY_PATTERN = Pattern.compile("text_opacity:([^,}]+)");
    private static final Pattern ALIGNMENT_PATTERN = Pattern.compile("alignment:\"([^\"]*)\"");
    private static final Pattern LINE_WIDTH_PATTERN = Pattern.compile("line_width:([^,}]+)");
    private static final Pattern DEFAULT_BACKGROUND_PATTERN = Pattern.compile("default_background:([^,}]+)");

    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private BdEngineModelConverter() {}

    /**
     * Asynchronously converts a bdengine model from a {@code function} folder to a JSON file.
     *
     * @param functionFolder the {@code function} folder (must exist and be a directory)
     * @param outputFile     the JSON file to write to (must not be a directory, must end with {@code .json})
     */
    public static void convert(File functionFolder, File outputFile) throws IOException {
        try {
            validateInputs(functionFolder, outputFile);

            Result result = new Result(new ArrayList<>(), new ArrayList<>());

            Map<String, SortedMap<Integer, String>> animations = readKeyframes(functionFolder.toPath());
            for (Map.Entry<String, SortedMap<Integer, String>> entry : animations.entrySet()) {
                String animationName = entry.getKey();
                SortedMap<Integer, String> frames = entry.getValue();

                List<KeyFrame> keyframes = new ArrayList<>();
                Animation animation = new Animation(animationName, keyframes);

                for (String frameData : frames.values()) {
                    KeyFrame keyFrame = new KeyFrame(readFrame(animationName, frameData));
                    animation.keyframes().add(keyFrame);
                }

                result.animations().add(animation);
            }

            result.nbt().addAll(readNbt(functionFolder.toPath()));

            Path outPath = outputFile.toPath();
            Files.createDirectories(outPath.getParent());
            Files.writeString(outPath, GSON.toJson(result), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert bdengine model", e);
        }
    }

    private static void validateInputs(File functionFolder, File outputFile) throws IOException {
        if (functionFolder == null || !functionFolder.isDirectory()) {
            throw new IllegalArgumentException("functionFolder must be an existing directory pointing at the datapack's function folder");
        }

        if (outputFile == null) throw new IllegalArgumentException("outputFile must not be null");
        if (outputFile.isDirectory()) throw new IllegalArgumentException("outputFile must not be a directory");
        if (!outputFile.getName().toLowerCase().endsWith(".json")) throw new IllegalArgumentException("outputFile name must end with .json");

        Path parent = outputFile.toPath().getParent();
        if (parent != null && !Files.exists(parent)) throw new IOException("Parent directory of outputFile does not exist: " + parent);
    }

    private static Map<String, SortedMap<Integer, String>> readKeyframes(Path functionFolder) throws IOException {
        Path kRoot = functionFolder.resolve("k");
        if (!Files.isDirectory(kRoot)) {
            return new HashMap<>();
        }

        Map<String, SortedMap<Integer, String>> anims = new HashMap<>();

        Files.walk(kRoot)
            .filter(Files::isRegularFile)
            .filter(p -> {
                String fn = p.getFileName().toString();
                return fn.startsWith("keyframe_") && fn.endsWith(".mcfunction");
            })
            .forEach(path -> {
                try {
                    String fileName = path.getFileName().toString();
                    String indexStr = fileName.substring("keyframe_".length(), fileName.length() - ".mcfunction".length());
                    int index = Integer.parseInt(indexStr);

                    String animName = path.getParent().getFileName().toString();

                    SortedMap<Integer, String> frames = anims.computeIfAbsent(animName, k -> new TreeMap<>());
                    frames.put(index, Files.readString(path, StandardCharsets.UTF_8));
                } catch (IOException | NumberFormatException e) {
                    throw new RuntimeException("Failed to read keyframe from " + path, e);
                }
            });

        return anims;
    }

    private static List<String> readNbt(Path functionFolder) throws IOException {
        Path createFile = functionFolder.resolve("_").resolve("create.mcfunction");
        if (!Files.isRegularFile(createFile)) throw new IOException("Missing '_/create.mcfunction' under function folder: " + createFile);

        List<String> nbtList = new ArrayList<>();
        List<String> lines = Files.readAllLines(createFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (!line.startsWith("execute as @e[tag=")) continue;

            int braceIndex = line.indexOf('{');
            if (braceIndex < 0) continue;

            String snbt = line.substring(braceIndex);
            String jsonish = snbtToJson(snbt);

            JsonElement element = GSON.fromJson(jsonish, JsonElement.class);

            if (element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                JsonArray passengers = root.getAsJsonArray("Passengers");
                if (passengers != null) {
                    for (JsonElement pEl : passengers) {
                        if (!pEl.isJsonObject()) continue;
                        JsonObject passenger = pEl.getAsJsonObject();
                        if (!passenger.has("id")) continue;
                        String id = passenger.get("id").getAsString();
                        if ("minecraft:text_display".equals(id) && passenger.has("text")) {
                            JsonElement textEl = passenger.get("text");
                            if (textEl.isJsonArray()) {
                                JsonArray arr = textEl.getAsJsonArray();
                                String arrString = GSON.toJson(arr);
                                passenger.addProperty("text", arrString);
                            }
                        }
                    }
                }
            }

            String out = GSON.toJson(element);
            nbtList.add(out);
        }
        return nbtList;
    }

    /**
     * Convert SNBT-like NBT from create.mcfunction into JSON that Gson can parse.
     * We carefully quote bare keys and normalize typed int arrays while leaving
     * quoted strings (including text components) untouched.
     */
    public static String snbtToJson(String snbt) {
        if (snbt == null || snbt.isEmpty()) return snbt;

        String[] keys = {
            "Passengers",
            "id",
            "item",
            "block_state",
            "item_display",
            "components",
            "minecraft:profile",
            "properties",
            "name",
            "value",
            "transformation",
            "Tags",
            "text",
            "text_opacity",
            "background",
            "alignment",
            "line_width",
            "default_background",
            "Count"
        };

        StringBuilder quoted = new StringBuilder(snbt.length() + 32);
        boolean inString = false;
        char stringQuote = 0;
        boolean escaping = false;

        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);

            if (inString) {
                quoted.append(c);
                if (escaping) escaping = false;
                else if (c == '\\') escaping = true;
                else if (c == stringQuote) inString = false;
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                stringQuote = c;
                quoted.append(c);
                continue;
            }

            if (snbt.startsWith("id:[I;", i)) {
                quoted.append("\"id\":[");
                i += "id:[I;".length() - 1;
                continue;
            }

            boolean matchedKey = false;
            for (String key : keys) {
                int len = key.length();
                if (i + len + 1 <= snbt.length()
                    && snbt.startsWith(key, i)
                    && snbt.charAt(i + len) == ':') {
                    quoted.append('"').append(key).append('"').append(':');
                    i += len;
                    matchedKey = true;
                    break;
                }
            }
            if (matchedKey) continue;

            quoted.append(c);
        }

        return quoted.toString();
    }

    private static List<FrameObject> readFrame(String anim, String data) {
        List<FrameObject> objects = new ArrayList<>();
        String[] lines = data.split("\\R");

        for (String line : lines) {
            if (!line.startsWith("data merge entity @e[type=")) continue;

            String[] tokens = line.split(" ", 5);
            if (tokens.length < 5) continue;
            String nbtPart = tokens[4];

            Matcher selectorMatcher = ENTITY_SELECTOR_PATTERN.matcher(line);
            if (!selectorMatcher.find()) continue;
            String selectorContent = selectorMatcher.group(1);

            Matcher typeMatcher = TYPE_PATTERN.matcher(selectorContent);
            Matcher tagMatcher = TAG_PATTERN.matcher(selectorContent);

            if (!typeMatcher.find() || !tagMatcher.find()) {
                continue;
            }

            String type = typeMatcher.group(1);
            String tag = tagMatcher.group(1);

            float[] transformation = null;
            String head = null;
            String text = null;
            Integer background = null;
            Integer textOpacity = null;
            String alignment = null;
            Float lineWidth = null;
            Boolean defaultBackground = null;

            Matcher transformationMatcher = TRANSFORMATION_PATTERN.matcher(line);
            if (transformationMatcher.find()) {
                String raw = transformationMatcher.group(1).replace("f", "");
                String[] parts = raw.split(",");
                float[] values = new float[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    try {
                        values[i] = Float.parseFloat(parts[i]);
                    } catch (NumberFormatException ignored) {}
                }
                transformation = values;
            }

            try {
                String jsonish = nbtPart
                    .replace("item:", "\"item\":")
                    .replace("components:", "\"components\":")
                    .replace("properties:", "\"properties\":")
                    .replace("name:", "\"name\":")
                    .replace("value:", "\"value\":");
                JsonElement element = GSON.fromJson(jsonish, JsonElement.class);
                if (element.isJsonObject()) {
                    JsonObject root = element.getAsJsonObject();
                    JsonObject item = root.getAsJsonObject("item");
                    if (item != null) {
                        JsonObject components = item.getAsJsonObject("components");
                        if (components != null) {
                            JsonObject profile = components.getAsJsonObject("minecraft:profile");
                            if (profile != null) {
                                JsonArray properties = profile.getAsJsonArray("properties");
                                if (properties != null) {
                                    for (JsonElement propEl : properties) {
                                        JsonObject prop = propEl.getAsJsonObject();
                                        if ("textures".equals(prop.get("name").getAsString())) {
                                            head = prop.get("value").getAsString();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (type.endsWith("text_display")) {

                Matcher textComponentsMatcher = TEXT_COMPONENT_PATTERN.matcher(nbtPart);
                if (textComponentsMatcher.find()) text = textComponentsMatcher.group(1);
                else {
                    Matcher legacyTextMatcher = LEGACY_TEXT_NBT_PATTERN.matcher(nbtPart);
                    if (legacyTextMatcher.find()) {
                        String legacy = legacyTextMatcher.group(1);
                        JsonObject obj = new JsonObject();
                        obj.addProperty("text", legacy);
                        text = obj.toString();
                    }
                }

                Matcher backgroundMatcher = BACKGROUND_PATTERN.matcher(nbtPart);
                if (backgroundMatcher.find()) {
                    try {
                        background = Integer.parseInt(backgroundMatcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }

                Matcher textOpacityMatcher = TEXT_OPACITY_PATTERN.matcher(nbtPart);
                if (textOpacityMatcher.find()) {
                    try {
                        textOpacity = Integer.parseInt(textOpacityMatcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }

                Matcher alignmentMatcher = ALIGNMENT_PATTERN.matcher(nbtPart);
                if (alignmentMatcher.find()) alignment = alignmentMatcher.group(1);

                Matcher lineWidthMatcher = LINE_WIDTH_PATTERN.matcher(nbtPart);
                if (lineWidthMatcher.find()) {
                    try {
                        lineWidth = Float.parseFloat(lineWidthMatcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }

                Matcher defaultBackgroundMatcher = DEFAULT_BACKGROUND_PATTERN.matcher(nbtPart);
                if (defaultBackgroundMatcher.find()) {
                    String value = defaultBackgroundMatcher.group(1).trim();
                    if ("true".equalsIgnoreCase(value)) defaultBackground = true;
                    else if ("false".equalsIgnoreCase(value)) defaultBackground = false;
                }
            }

            Commands commands = new Commands(transformation, head, text,
                background, textOpacity, alignment, lineWidth, defaultBackground);
            FrameObject obj = new FrameObject(type, tag, commands);
            objects.add(obj);
        }

        return objects;
    }

    private record Commands(
        float[] transformation,
        String head,
        String text,
        Integer background,
        Integer textOpacity,
        String alignment,
        Float lineWidth,
        Boolean defaultBackground
    ) { }

    private record FrameObject(String type, String tag, Commands commands) { }

    private record KeyFrame(List<FrameObject> objects) { }

    private record Animation(String name, List<KeyFrame> keyframes) { }

    private record Result(List<String> nbt, List<Animation> animations) { }

}