package model;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;

public class LoadManager {
    public static GameSave loadGame() {
        Gson gson = new Gson();

        try {
            FileReader reader = new FileReader("save.json");
            GameSave save = gson.fromJson(reader, GameSave.class);
            reader.close();

            return save;
        } catch (IOException e) {
            System.out.println("Error while loading game.");
            return null;
        }
    }
}