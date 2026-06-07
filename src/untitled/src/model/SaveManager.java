package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

public class SaveManager {
    public static void saveGame(GameSave save) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            FileWriter writer = new FileWriter("save.json");
            gson.toJson(save, writer);
            writer.close();

            System.out.println("Game saved!");
        } catch (IOException e) {
            System.out.println("Error while saving game.");
        }
    }
}
