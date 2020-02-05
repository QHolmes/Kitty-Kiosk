package core;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {

    public static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), "Kitty Kiosk");

    public static final int RESULTS_LIMIT = 1000;

}
