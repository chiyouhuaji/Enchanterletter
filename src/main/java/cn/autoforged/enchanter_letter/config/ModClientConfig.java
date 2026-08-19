package cn.autoforged.enchanter_letter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "enchanter_letter_client.json";

    /** HUD 循环模式：0=伤害增益 1=护甲值增益 2=护甲韧性增益 3=抗性提升增益（关闭时跳过）4=关闭。 */
    @SerializedName("hud_mode")
    public int hudMode = 0;

    @SerializedName("hud_toggle_key_code")
    public int hudToggleKeyCode = org.lwjgl.glfw.GLFW.GLFW_KEY_N;

    private static ModClientConfig instance;

    public static ModClientConfig getInstance() {
        if (instance == null) {
            instance = new ModClientConfig();
        }
        return instance;
    }

    public static void init() {
        Path configDir = Path.of("config");
        Path configFile = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile);
                instance = GSON.fromJson(content, ModClientConfig.class);
                if (instance == null) instance = new ModClientConfig();
            } else {
                instance = new ModClientConfig();
                Files.createDirectories(configDir);
                Files.writeString(configFile, GSON.toJson(instance));
            }
        } catch (IOException e) {
            instance = new ModClientConfig();
        }
    }

    public static void save() {
        Path configDir = Path.of("config");
        Path configFile = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, GSON.toJson(getInstance()));
        } catch (IOException ignored) {
        }
    }
}
