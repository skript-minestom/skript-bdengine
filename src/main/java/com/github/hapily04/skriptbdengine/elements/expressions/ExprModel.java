package com.github.hapily04.skriptbdengine.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptbdengine.SkriptBDEngine;
import com.github.hapily04.skriptbdengine.api.AnimationModel;
import com.github.hapily04.skriptbdengine.api.BdEngineModelConverter;
import com.github.hapily04.skriptbdengine.api.ModelManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ExprModel extends SimpleExpression<AnimationModel> {

    static {
        Skript.registerExpression(ExprModel.class, AnimationModel.class, ExpressionType.COMBINED,
            "[new] [animation] model (from|with) [id] %string%");
    }

    private Expression<String> modelId;

    @SuppressWarnings("unchecked")
    private static boolean isFunctionFolderNewer(File functionFolder, File convertedFile) throws IOException {
        long convertedAt = convertedFile.lastModified();
        try (var paths = Files.walk(functionFolder.toPath())) {
            return paths.filter(Files::isRegularFile)
                .anyMatch(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() > convertedAt;
                    } catch (IOException e) {
                        return false;
                    }
                });
        }
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        modelId = (Expression<String>) expressions[0];
        return true;
    }

    @Override
    protected AnimationModel[] get(Event event) {
        String modelId = this.modelId.getSingle(event);
        if (modelId == null) return new AnimationModel[0];
        SkriptBDEngine addon = SkriptBDEngine.getInstance();
        try {
            return new AnimationModel[]{getAnimationModel(addon, modelId)};
        } catch (Exception e) {
            addon.logger().error("An error occured whilst attempting to get model with id {}: {}", modelId, e.getMessage());
            e.printStackTrace();
            return new AnimationModel[0];
        }
    }

    private AnimationModel getAnimationModel(SkriptBDEngine addon, String modelId) throws IOException {
        ModelManager modelManager = addon.getModelManager();
        File convertedFile = new File(addon.getConvertedFolder(), modelId + ".json");
        File functionFolder = new File(addon.getFunctionsFolder(), modelId);
        if (functionFolder.isDirectory() && functionFolder.exists()
            && (!convertedFile.exists() || isFunctionFolderNewer(functionFolder, convertedFile))) {
            BdEngineModelConverter.convert(functionFolder, convertedFile);
        }
        if (!convertedFile.exists()) return null;
        return modelManager.getAnimationModel(modelId).join();
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends AnimationModel> getReturnType() {
        return AnimationModel.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "new animation model from id " + modelId.toString(event, debug);
    }

}
