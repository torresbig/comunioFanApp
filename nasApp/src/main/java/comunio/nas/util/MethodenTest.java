package comunio.nas.util;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.Login;

public class MethodenTest {

//	
//	https://www.comunio.de/api/communities/884691/users/5981249/lineup
//		testabfrage bauen, ob ich alle user abfragen kann, da dort der status raus kommt. wenn nicht, dann wenigstens für meine spieler oder zukünftig für die handyapp
//	13548242
	// lineup leider nur für die eingene playerID .Kann aber zukünftig für abfragen
	// wenn sich jeder user selbst einloggt, sinnvoll sein.
//	
	public static JSONObject fetchSpielerJson() {
		try {

			String url = "https://www.comunio.de/api/communities/884691/users/13548242/lineup";
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

			return playerData;
		} catch (Exception e) {
			return null;
		}

	}

}
