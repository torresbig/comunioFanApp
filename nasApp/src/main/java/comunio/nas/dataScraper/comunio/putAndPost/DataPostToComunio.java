package comunio.nas.dataScraper.comunio.putAndPost;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.Login;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;

public class DataPostToComunio {

	private static final Logger LOGGER = LogManager.getLogger(DataPostToComunio.class);

	/**
	 * Fügt Spieler dem Markt hinzu mit Preis = 115 % des aktuellen Spielerwerts.
	 *
	 * @param playerList JSONArray mit JSONObject-Einträgen, jedes mit Unterobjekt
	 *                   "data" und Integer "wert"
	 * @return true bei erfolgreicher Aktion, false sonst
	 */
	public static boolean addListOfPlayersToMarket(JSONArray playerList, User user) {
		JSONArray items = new JSONArray();
		for (Object arrObj : playerList) {
			if (!(arrObj instanceof JSONObject)) {
				LOGGER.warning("Ungültiger Spieler-Eintrag (kein JSONObject): " + arrObj);
				continue;
			}
			JSONObject player = (JSONObject) arrObj;

			if (!player.has("id") || !player.has("data")) {
				LOGGER.warning("Spieler-Eintrag fehlt 'id' oder 'data': " + player);
				continue;
			}
			JSONObject data = player.getJSONObject("data");
			if (!data.has("wert")) {
				LOGGER.warning("Spieler-'data' hat kein 'wert': " + player);
				continue;
			}

			int tradableId = player.getInt("id");
			int wert = data.getInt("wert");

			// 15% Aufschlag berechnen und als String setzen
			int priceValue = (int) Math.round(wert * 1.15);
			String price = String.valueOf(priceValue);

			JSONObject item = new JSONObject();
			item.put("tradableId", tradableId);
			item.put("price", price);

			items.put(item);
		}

		JSONObject payload = new JSONObject().put("items", items);
		String url = Urls.COM_API(user) + "/exchangemarket/addplayer";
		return sendPostRequest(url, payload, user);
	}
	
	public static boolean addPlayerToMarket(JSONObject player, User user) {
		return addListOfPlayersToMarket(new JSONArray().put(player), user);

	}

	/**
	 * Remove Player vom Transfermarkt
	 * https://www.comunio.de/api/communities/884691/users/5981249/exchangemarket/removeplayer
	 * {"tradableIds":[32792]} response {"status":"OK"}
	 */

	public static boolean removePlayerFromMarket(JSONObject player, User user) {
		return removeListOfPlayersFromMarket(new JSONArray().put(player), user);

	}

	/**
	 * Entfernt Spieler vom Markt.
	 *
	 * @param playerList JSONArray mit JSONObject-Einträgen mit Feld "id"
	 * @return true bei Erfolg, false sonst
	 */
	public static boolean removeListOfPlayersFromMarket(JSONArray playerList, User user) {
		JSONArray ids = new JSONArray();
		for (Object arrObj : playerList) {
			if (!(arrObj instanceof JSONObject) || !((JSONObject) arrObj).has("id")) {
				LOGGER.warning("Ungültiger Spieler-Eintrag: " + arrObj);
				continue;
			}
			ids.put(((JSONObject) arrObj).getInt("id"));
		}
		JSONObject payload = new JSONObject().put("tradableIds", ids);
		String url = Urls.COM_API(user) + "/exchangemarket/removeplayer";
		return sendPostRequest(url, payload, user);
	}

	/**
	 * Führt den eigentlichen POST-Request mit gegebenem URL und Payload durch.
	 *
	 * @param url     Endpunkt-URL für die API
	 * @param payload JSON-Daten für den Request-Body
	 * @param LOGGER  Logger für das Logging
	 * @return true, wenn das Feld "status" in der Response "OK" ist, sonst false
	 */
	private static boolean sendPostRequest(String url, JSONObject payload, User user) {
		StringBuilder logBuilder = new StringBuilder();
		logBuilder.append("Sende Post an Comunio: " + url + " - Payload: " + payload.toString()).append("\n");
		Login.login(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString(), ComunioDataUpdater.community, user); // Stelle gültiges Token sicher
		boolean result = false;
		try {
			HttpClient client = HttpClient.newBuilder()//
				    .followRedirects(HttpClient.Redirect.NORMAL)//
				    .build();

				HttpRequest request = HttpRequest.newBuilder()//
				    .uri(URI.create(url))//
				    .header("Accept", "application/json, text/plain, */*")//
				    .header("Authorization", "Bearer " + Login.getToken())//
				    .header("Accept-Encoding", "identity") // nur dieser Header
				    .header("Accept-Language", "de-DE,en-EN;q=0.9")//
				    .header("x-timezone", "Europe/Berlin")//
				    .header("Content-Type", "application/json;charset=UTF-8")//
				    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))//
				    .build();

				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			
//			logBuilder.append("HTTP Status: ").append(response.statusCode()).append("\n");
//			logBuilder.append("Response-Body: ").append(response.body()).append("\n");
//			logBuilder.append("Response-Headers: ").append(response.headers().map());


			if (response.statusCode() == 200) {
				JSONObject jResult = new JSONObject(response.body());
				result = jResult.optString("status").equals("OK");
				logBuilder.append("Spieler erfolgreich auf den Markt gesetzt oder gelöscht! Payload: " + payload);
			} else {
				logBuilder.append("WARNUNG: API-Fehler: Statuscode " + response.statusCode());
			}
		} catch (IOException | InterruptedException e) {
			logBuilder.append("WARNUNG: HTTP-Request fehlgeschlagen: " + e.getMessage());
		}
		LOGGER.info(logBuilder.toString());
		return result;
	}


}
