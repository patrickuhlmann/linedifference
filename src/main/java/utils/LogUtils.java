package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class LogUtils {
    public static void initalizeLogging(String resource) {
        InputStream stream = LogUtils.class.getClassLoader().
                getResourceAsStream(resource);
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            System.out.println("Unable to load logging configuration.");
            e.printStackTrace();
        }
    }
}