package com.github.hapily04.skriptbdengine.api;

import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.hapily04.skriptbdengine.api.BdEngineModelConverter.GSON;

public class ModelManager {

    private final File modelDirectory;
    private final Map<String, AnimationModel.JSON.Data> models = new ConcurrentHashMap<>();

    public ModelManager(File modelDirectory) {
        this.modelDirectory = modelDirectory;
    }

    public CompletableFuture<AnimationModel> getAnimationModel(String modelName) {
        if (models.containsKey(modelName)) return CompletableFuture.completedFuture(new AnimationModel(models.get(modelName)));
        return CompletableFuture.supplyAsync(() -> {
            try {
                AnimationModel.JSON.Data data = getData(modelName);
                models.put(modelName, data);
                return new AnimationModel(data);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private AnimationModel.JSON.Data getData(String modelName) throws FileNotFoundException {
        File file = new File(modelDirectory, modelName + ".json");
        if (!file.exists()) throw new FileNotFoundException("Json file not found for model under the name '" + modelName + "'.");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            String string = sb.toString();
            return GSON.fromJson(string, AnimationModel.JSON.Data.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
