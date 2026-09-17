package comunio.nas.objects.helper;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import java.io.File;


public class LogManager {
    private static final Logger ROOT_LOGGER = Logger.getLogger("");

    static {
        try {
            // Absolute Pfad-Referenz im Container
            File logDir = new File("/app/log");
            
            // Falls der Ordner noch nicht existiert oder Rechte fehlen, explizit erstellen
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // Pfad: /app/log/comunio_updater%g.log
            // Limit: 5 MB pro Datei, bewahrt maximal 5 Dateien auf
            FileHandler fh = new FileHandler("/app/log/comunio_updater%g.log", 5 * 1024 * 1024, 5, true);
            fh.setFormatter(new SimpleFormatter());

            ROOT_LOGGER.addHandler(fh);
            ROOT_LOGGER.setLevel(Level.ALL);
        } catch (Throwable t) {
            // Falls wider Erwarten doch ein Fehler auftritt, landet er in der Konsole
            System.err.println("Fehler beim Initialisieren des LogHandlers:");
            t.printStackTrace();
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }
}