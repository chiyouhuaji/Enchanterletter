package cn.autoforged.enchanter_letter.storage;

/**
 * /letterstorage 客户端状态：标记是否处于“截取实体”交互模式。
 * 由服务端在 /letterstorage 切换时通过 S2C 包同步，客户端据此拦截空手左键。
 */
public class LetterStorageClient {
    private static volatile boolean active = false;

    private LetterStorageClient() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean on) {
        active = on;
    }
}
