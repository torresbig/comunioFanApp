package comunio.nas.objects.helper;

import java.util.logging.Logger;


public class LoadingTimeManager {

	
	private static Logger LOGGER = null;
	
	static long startTime = System.nanoTime();  // Startzeit für die Performance-Messung
	static long endTime = System.nanoTime();    // Endzeit für die Messung
	
	
	
	
	
	static <T> void starteMessung(Class<T> klasse, String text) {
		startTime = System.nanoTime();
		LOGGER = LogManager.getLogger(klasse);
		
	}
	
	static void endMessung(String text) {
		endTime = System.nanoTime();
		LOGGER.info("Updater erfolgreich abgeschlossen. \nLadezeit komplettes Programm: " + (endTime - startTime) / 1_000_000 + " ms");
		
	}
	
}
