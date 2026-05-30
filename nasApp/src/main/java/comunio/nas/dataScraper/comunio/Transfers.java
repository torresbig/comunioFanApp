package comunio.nas.dataScraper.comunio;

import java.util.logging.Logger;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.HttpHeaderUtil;

public class Transfers {
	
	
	private static final Logger LOGGER = LogManager.getLogger(PlayerUpdater.class);
	
	public static void main(String[] args) {
		fetchSpielerJson("5981249"); // zum testen!
		
	}
	
	
	public static void loadAllTransfersForUser(String userID) {
		//https://www.comunio.de/api/communities/884691/users/5981249/offers?type=PURCHASE&limit=10&sort=datechanged
		
	}
	
	private static JSONObject fetchSpielerJson(String userID) {
		try {

			String url = "https://www.comunio.de/api/communities/" + ComunioDataUpdater.community.getId() + "/users/" + userID + "/offers?type=PURCHASE&limit=10&sort=datechanged";
			Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
			String jsonResponse = Jsoup.connect(url)//
					.userAgent(HttpHeaderUtil.getRandomUserAgent())//
					.header("Accept", "application/json, text/plain, */*")//
					.header("Authorization", "Bearer " + Login.getToken())//
					.header("Accept-Encoding", "gzip, deflate, br, zstd")//
					.header("Accept-Language", "de-DE,en-EN;q=0.9")//
					.header("x-timezone", "Europe/Berlin")//
					.ignoreContentType(true)//
					.execute()//
					.body();

			JSONObject playerData = new JSONObject(jsonResponse);

			LOGGER.info("");

			return playerData;
		} catch (Exception e) {
			LOGGER.warning("" + e.getMessage());
			return null;
		}

	}

}
