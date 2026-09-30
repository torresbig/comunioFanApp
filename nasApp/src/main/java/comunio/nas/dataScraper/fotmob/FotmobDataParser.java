package comunio.nas.dataScraper.fotmob;

import comunio.nas.objects.fotmob.FotmobPlayerDataObject;
import comunio.nas.objects.helper.LogManager;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser for fetching and processing FotMob player data.
 */
public class FotmobDataParser {
	private static final Logger LOGGER = LogManager.getLogger(FotmobDataParser.class);
	private static final String PLAYER_DATA_URL = "https://www.fotmob.com/api/data/playerData?id=%s";
	private static final int MAX_RETRIES = 3;
	private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
	
	public static void main(String[] args) {
	parseFotmobPlayerData("1179182");
	}

	/**
	 * Parses FotMob player data for the given player ID.
	 *
	 * @param playerId the player ID as a string
	 * @return parsed FotmobPlayerDataObject or null if parsing fails
	 */
	public static FotmobPlayerDataObject parseFotmobPlayerData(String playerId) {
		String url = String.format(PLAYER_DATA_URL, playerId);
		JSONObject json = fetchPlayerDataWithRetry(url);

		if (json == null) {
			LOGGER.warning("Failed to fetch player data for ID: " + playerId);
			return null;
		}

		try {
			FotmobPlayerDataObject result = FotmobPlayerDataObject.fromJSON(json);
			return result;
		} catch (Exception e) {
			LOGGER.warning("Error parsing player data for ID:  " + playerId + " | " + e.getMessage());
			return null;
		}
	}

	/**
	 * Fetches player data from the API with retry logic.
	 *
	 * @param url the API URL
	 * @return JSONObject containing player data or null if all retries fail
	 */
	private static JSONObject fetchPlayerDataWithRetry(String url) {
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				LOGGER.log(Level.FINE, "Fetching player data from: " + url + " (Attempt " + attempt + "/" + MAX_RETRIES + "");

				Document doc = Jsoup.connect(url)//
						.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")//
						.ignoreContentType(true)//
						.timeout(10000)//
						.get();
				

				String responseBody = doc.body().text();
				if (responseBody != null && !responseBody.isEmpty()) {
					return new JSONObject(responseBody);
				}

				LOGGER.log(Level.WARNING, "Empty response received from: " + url);
				return null;

			} catch (IOException e) {
				LOGGER.warning("IOException on attempt " + attempt + "/" + MAX_RETRIES + " for URL: " + url + " - " + e.getMessage());

				if (attempt < MAX_RETRIES) {
					try {
						Thread.sleep(RETRY_DELAY.toMillis());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						LOGGER.log(Level.SEVERE, "Thread interrupted during retry delay - " + ie);
						return null;
					}
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Unexpected error fetching data from: " + url + " | " + e.getMessage());
				return null;
			}
		}

		LOGGER.log(Level.SEVERE, "All " + MAX_RETRIES + " attempts failed for URL: " + url);
		return null;
	}
}
