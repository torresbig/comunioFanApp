package comunio.nas.objects.helper;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;


public class MarketvalueFixer {

	private static final Logger LOGGER = LogManager.getLogger(MarketvalueFixer.class);
	
	/**
	 * Bereinigt die gesamte Marktwertdatenbank von Datumsduplikaten.
	 * 
	 * @param marketValueDB Marktwertdatenbank
	 */
	public static void cleanMarketValueDatabase(JSONArray marketValueDB) {
		LOGGER.info("Starte Bereinigung der Marktwertdatenbank...");
		for (int i = 0; i < marketValueDB.length(); i++) {
			JSONObject player = marketValueDB.getJSONObject(i);
			JSONArray normalValues = player.getJSONObject("data").getJSONArray("normal");

			Set<String> uniqueDates = new HashSet<>();
			JSONArray cleanedValues = new JSONArray();
			int removed = 0;

			for (int j = 0; j < normalValues.length(); j++) {
				JSONObject entry = normalValues.getJSONObject(j);
				String date = entry.getString("date");

				if (!uniqueDates.contains(date)) {
					cleanedValues.put(entry);
					uniqueDates.add(date);
				} else {
					removed++;
					LOGGER.fine("Duplikat entfernt: " + player.getString("id") + " am " + date);
				}
			}

			player.getJSONObject("data").put("normal", cleanedValues);
			if (removed > 0) {
				LOGGER.info("Für Spieler " + player.getString("id") + " wurden " + removed + " Duplikate entfernt.");
			}
		}
		LOGGER.info("Bereinigung der Marktwertdatenbank abgeschlossen.");
	}
	
}
