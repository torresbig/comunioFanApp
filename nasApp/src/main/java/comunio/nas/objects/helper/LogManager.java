package comunio.nas.objects.helper;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogManager {
    private static final Logger ROOT_LOGGER = Logger.getLogger("");

    static {
        try {
            FileHandler fh = new FileHandler("comunio_updater.log", false);
            fh.setFormatter(new SimpleFormatter());
            ROOT_LOGGER.addHandler(fh);
            ROOT_LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }
}

