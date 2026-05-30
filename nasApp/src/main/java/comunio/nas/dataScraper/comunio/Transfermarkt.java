package comunio.nas.dataScraper.comunio;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import comunio.nas.dataScraper.comunio.putAndPost.DataPostToComunio;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;
import comunio.nas.util.DateUtils;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.player.PlayerHelper;

public class Transfermarkt {

	private static final Logger LOGGER = LogManager.getLogger(Transfermarkt.class);

	/**
	 * Ruft die Liste der Spieler auf dem Transfermarkt mit aktuellen Marktdaten ab.
	 * Liest die Spieler aus dem übergebenen playerDbOject und verknüpft sie mit den
	 * Daten aus der Comunio-API.
	 * 
	 * @param playerDbOject JSON-Objekt mit dem Array "playerDB" der
	 *                      Spieler-Datenbank.
	 * @return JSONArray mit Informationen zu Spielern auf dem Transfermarkt
	 *         (playerID, playerName, Preis, verbleibende Zeit etc.) oder null,
	 *         falls Spieler-Datenbank nicht vorhanden.
	 */
	public static JSONArray getTransfermarktListe(JSONObject playerDbOject, JSONArray transfermarktListe, JSONObject notInligaDBObj, LastUpdates lastUpdates, User user) {
		// https://www.comunio.de/api/communities/884691/users/5981249/exchangemarket?include=trend,direct

		JSONArray playerDB = playerDbOject.optJSONArray("playerDB");
		if (playerDB == null) {
			return transfermarktListe;
		}
		try {

			String url = Urls.COM_API(user) + "/exchangemarket?include=trend,direct";
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

			JSONObject dataObj = new JSONObject(jsonResponse);

			if (dataObj != null) {
				transfermarktListe.clear();

				dataObj.optString("nextTransfersDateTime", null);

				JSONArray jArray = dataObj.getJSONArray("items");
				for (Object jo : jArray) {
					JSONObject json = (JSONObject) jo;
					json.getInt("remaining");
					String setOnMarket = json.getString("date");
					json = json.getJSONObject("_embedded");
					JSONObject owner = json.getJSONObject("owner");
					String.valueOf(owner.optInt("id", 1));
					json = json.getJSONObject("player");
					String id = String.valueOf(json.getInt("id"));
					String name = json.getString("name");
					int marktwert = json.getInt("quotedPrice");
					int preis = json.getInt("recommendedPrice");
					JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, id, notInligaDBObj);

					if (player != null) {
						JSONObject data = player.optJSONObject("data");
						if (data == null) {
							continue;
						}
						// Marktwerte nicht abfragen, da sonst ggf. nicht die änderungen vom marktwert
						// erkannt wird. oder hier muss die richtige methode für addMarktwert ist
//						data.put("wert", marktwert);

						JSONObject playerOnMarket = new JSONObject();
						playerOnMarket.put("playerID", id);
						playerOnMarket.put("playerName", name);
						playerOnMarket.put("date", DateUtils.getNowToString());
						playerOnMarket.put("preis", preis);

						String remainingDate = getDeadlineDateTime(setOnMarket);
						long remainingSeconds = calculateRemainingSeconds(setOnMarket);

						playerOnMarket.put("remainingDate", remainingDate);
						playerOnMarket.put("remainingSeconds", remainingSeconds);
//						playerOnMarket.put("nextTransfersDateTime", nextTransfersDateTime);

						playerOnMarket.put("setOnMarket", setOnMarket);
						// zusätzliche Infos für die Tabelle
						playerOnMarket.put("verein", data.optString("verein", "0"));
						playerOnMarket.put("punkte", data.optInt("punkte", 0));
						playerOnMarket.put("position", data.optString("position", "UNBESTIMMT"));
						playerOnMarket.put("wert", marktwert);
						JSONObject status = data.optJSONObject("status", new JSONObject());
						playerOnMarket.put("status", status.optString("status", "UNBEKANNT"));

						transfermarktListe.put(playerOnMarket);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.severe("Fehler beim Abrufen der Transfermarkt-Liste: " + e.getMessage());
			LOGGER.throwing(Transfermarkt.class.getName(), "getTransfermarktListe", e);
		}
		lastUpdates.setTransfermarktList(Instant.now());
		return transfermarktListe;
	}

	private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

	/**
	 * Berechnet das exakte Ablaufdatum und die Uhrzeit ("Deadline") eines Transfers
	 * auf dem Transfermarkt anhand des Markt-Setzens.
	 *
	 * Die Deadline ist definiert als der dritte 3-Uhr-Batch nach dem Setzen.
	 *
	 * @param setOnMarket ISO8601 Zeitstempel des Markt-Setzens, z.B.
	 *                    "2025-08-30T23:57:48+0200"
	 * @return Deadline als String im ISO8601-Format, z.B.
	 *         "2025-09-02T03:00:00+0200"
	 */
	public static String getDeadlineDateTime(String setOnMarket) {
		// Normiere Zeitzonenformat: +0200 → +02:00
		String normalized = setOnMarket.replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2");

		// Parse String zu OffsetDateTime (Datum, Zeit und Offset)
		OffsetDateTime start = OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		// Erster 3-Uhr-Termin nach dem Setzen
		OffsetDateTime next3am = start.withHour(3).withMinute(0).withSecond(0).withNano(0);
		if (!start.isBefore(next3am)) {
			next3am = next3am.plusDays(1);
		}

		// Deadline = dritter 3-Uhr-Termin → +2 Tage
		OffsetDateTime deadline = next3am.plusDays(2);
		
		if(deadline.isBefore( OffsetDateTime.now())) {
			deadline = deadline.plusDays(1);
		}

		return deadline.format(OUTPUT_FORMAT);
	}

	/**
	 * Berechnet die verbleibende Zeit in Sekunden bis zur Deadline, ohne das
	 * unzuverlässige „remaining“ aus der API zu nutzen.
	 *
	 * @param setOnMarket ISO8601 Zeitstempel des Markt-Setzens
	 * @return verbleibende Sekunden bis Ablauf, oder 0 falls überschritten
	 */
	public static long calculateRemainingSeconds(String setOnMarket) {
		String normalized = setOnMarket.replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2");
		OffsetDateTime start = OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		// Erster 3-Uhr-Termin nach dem Setzen
		OffsetDateTime next3am = start.withHour(3).withMinute(0).withSecond(0).withNano(0);
		if (!start.isBefore(next3am)) {
			next3am = next3am.plusDays(1);
		}

		// Deadline = dritter 3-Uhr-Termin nach Setzen
		OffsetDateTime deadline = next3am.plusDays(2);

		// Aktueller Zeitpunkt im gleichen Offset
		OffsetDateTime now = OffsetDateTime.now(start.getOffset());

		long seconds = Duration.between(now, deadline).getSeconds();
		return Math.max(seconds, 0);
	}

	// Beispielnutzung
	public static void main(String[] args) {
		String setOnMarket = "2025-09-01T04:35:27+0200";
		System.out.println("Deadline: " + getDeadlineDateTime(setOnMarket));
		System.out.println("Remaining: " + calculateRemainingSeconds(setOnMarket) + "s");
		setOnMarket = "2025-09-01T23:35:27+0200";
		System.out.println("Deadline: " + getDeadlineDateTime(setOnMarket));
		System.out.println("Remaining: " + calculateRemainingSeconds(setOnMarket) + "s");
		setOnMarket = "2025-09-01T01:35:27+0200";
		System.out.println("Deadline: " + getDeadlineDateTime(setOnMarket));
		System.out.println("Remaining: " + calculateRemainingSeconds(setOnMarket) + "s");
		setOnMarket = "2025-09-01T14:35:27+0200";
		System.out.println("Deadline: " + getDeadlineDateTime(setOnMarket));
		System.out.println("Remaining: " + calculateRemainingSeconds(setOnMarket) + "s");
	}

	/**
	 * Ruft die aktuelle Liste der Gebote auf dem Transfermarkt ab. Verknüpft dabei
	 * die Gebote mit der übergebenen Spieler-Datenbank.
	 * 
	 * @param playerDbOject JSON-Objekt mit dem Array "playerDB" der
	 *                      Spieler-Datenbank.
	 * @return JSONArray mit aktuellen Geboten inklusive Bieter- und Anbieterdetails
	 *         oder null, falls Spieler-Datenbank nicht vorhanden.
	 */

	public static JSONArray getTransfermarktGeboteListe(JSONObject playerDbOject, JSONObject notInligaDBObj, User user) {
		JSONArray resultList = new JSONArray();

		JSONArray playerDB = playerDbOject.optJSONArray("playerDB");
		if (playerDB == null) {
			return null;
		}

		try {
			String url = Urls.COM_API(user) + "/offers?current";
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

			JSONObject dataObj = new JSONObject(jsonResponse);

			if (dataObj != null) {

				JSONArray jArray = dataObj.getJSONArray("items");
				for (Object jo : jArray) {
					JSONObject json = (JSONObject) jo;
					int offerId = json.optInt("id", 0);
					int gebot = json.optInt("price", 0);
					String bieter = json.getJSONObject("user").getString("name");
					String bieterID = String.valueOf(json.getJSONObject("user").getInt("id"));
					String anbieter = json.getJSONObject("tradingPartner").getString("name");
					String anbieterID = String.valueOf(json.getJSONObject("tradingPartner").getInt("id"));
					String dateChanged = json.getString("datechanged");
					String state = json.getString("state");
					String type = json.getString("type");
					String dateCreated = json.getString("datecreated");
					int damaligerKaufpreis = json.optInt("purchasePrice", 0); // TODO: hiermit kann man rechnen, wieviel man plus oder minus gemacht hat

					JSONObject tradable = json.getJSONObject("tradable");
					String id = String.valueOf(tradable.getInt("id"));
					String name = tradable.getString("name");
					String.valueOf(tradable.get("status"));

					int marktwert = tradable.optInt("quotedPrice", 0);
					int mindestGebot = tradable.optInt("recommendedPrice", 0);

					JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, id, notInligaDBObj);
					if (player != null) {
						JSONObject data = player.optJSONObject("data");
						if (data == null) {
							return null;
						}
						// Marktwerte nicht abfragen, da sonst ggf. nicht die änderungen vom marktwert
						// erkannt wird. oder hier muss die richtige methode für addMarktwert ist
//						data.put("wert", marktwert);

						JSONObject playerOnMarket = new JSONObject();
						playerOnMarket.put("playerID", id);
						playerOnMarket.put("playerName", name);
						playerOnMarket.put("dateChanged", dateChanged);
						playerOnMarket.put("dateCreated", dateCreated);
						playerOnMarket.put("date", DateUtils.getNowToString());
						playerOnMarket.put("gebot", gebot);
						playerOnMarket.put("bieter", bieter);
						playerOnMarket.put("bieterID", bieterID);
						playerOnMarket.put("anbieter", anbieter);
						playerOnMarket.put("anbieterID", anbieterID);
						playerOnMarket.put("offerId", offerId);
						playerOnMarket.put("marktwert", marktwert);
						playerOnMarket.put("mindestGebot", mindestGebot);
						playerOnMarket.put("state", state);
						playerOnMarket.put("type", type);
						playerOnMarket.put("damaligerKaufpreis", damaligerKaufpreis);

						resultList.put(playerOnMarket);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.severe("Fehler beim Abrufen der Transfermarkt-Gebotsliste: " + e.getMessage());
			LOGGER.throwing(Transfermarkt.class.getName(), "getTransfermarktGeboteListe", e);
		}

		return resultList;
	}

	/**
	 * Prüft automatisch alle Gebote und akzeptiert oder lehnt diese ab, basierend
	 * auf vordefinierten Wertgrenzen (z.B. unter 168.000 ablehnen).
	 * 
	 * @param playerDBObject JSON-Objekt mit Spieler-Datenbank mit Spieler-Details.
	 */
	public static void acceptOrDecline160erOffer(JSONObject playerDBObject, User user, boolean scheissAufsGuthaben, JSONObject notInligaDBObj) {
		long guthaben = 0;
		LOGGER.info("acceptOrDecline160erOffer - Check der Angebote wird gestartet...");
		if (user == null || user.getId().isBlank()) {
			return;
		}
		guthaben = user.getGuthaben();
		if (guthaben < 0 && !scheissAufsGuthaben) {
			return;
		}

		JSONArray gebotsListe = getTransfermarktGeboteListe(playerDBObject, notInligaDBObj, user);
		
		if(gebotsListe.isEmpty()) {
			LOGGER.info("Keine Transfer-Angebote vorhanden");
			return;
		}
		
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			return;
		}
		JSONArray listOf160er = new JSONArray();
		for (Object obj : gebotsListe) {
			JSONObject offer = (JSONObject) obj;
			JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, offer.getString("playerID"), notInligaDBObj);
			JSONObject data = player.optJSONObject("data");
			if (data == null) {
				continue;
			}
			int wert = data.optInt("wert", 0);
			// TODO: ggf. noch für alle transfers machen. dass immer wenn das gebot Unter
			// wert ist, außer man ist im Minus
			if (offer.has("gebot") && (offer.getInt("gebot") < 168000)) {
				// ablehnen!
				offer.put("type", "DECLINE");
				listOf160er.put(offer);

			} else if (offer.has("gebot") && wert == 160000 && offer.getInt("gebot") > wert && offer.getInt("gebot") >= 168000) {
				offer.put("type", "ACCEPT");
				listOf160er.put(offer);
			}
		}
		if (listOf160er.length() > 0) {
			submitTransferForPlayer(listOf160er, user);
			for (Object obj : listOf160er) {
				JSONObject jsonObj = (JSONObject) obj;
				if (jsonObj.has("type") && jsonObj.getString("type").equals("DECLINE")) {
					JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, jsonObj.getString("playerID"), notInligaDBObj);
					if (player != null) {
//					DataPostToComunio.removePlayerFromMarket(player);
						DataPostToComunio.addPlayerToMarket(player, user);
						
					}
				}
			}
		}
	}


	/**
	 * Übermittelt eine Liste von Transfer-Operationen (Akzeptieren, Ablehnen,
	 * Löschen, Ändern) an die Comunio-API.
	 * 
	 * @param transfers JSONArray mit Transfer-JSON-Objekten, die jeweils folgende
	 *                  Felder enthalten sollten: - "gebot" (Preis) - "type"
	 *                  (TRANSFER-Typ: ACCEPT, DECLINE, DELETE, CHANGE) - "playerID"
	 *                  (Spieler-ID) - optional "offerId" (Angebots-ID)
	 * 
	 * @return true, wenn der Transfer erfolgreich war, sonst false.
	 */
	public static boolean submitTransferForPlayer(JSONArray transfers, User user) {
		String typ = "";
		boolean result = false;
		JSONObject data = new JSONObject();
		JSONArray transArray = new JSONArray();
		for (Object tran : transfers) {
			JSONObject inputTran = (JSONObject) tran;
			JSONObject transJson = new JSONObject();
			transJson.put("price", inputTran.getInt("gebot"));
			typ = inputTran.getString("type");
			transJson.put("type", typ);
			transJson.put("tradableid", Integer.valueOf(inputTran.getString("playerID")));
			if (typ.equalsIgnoreCase("DELETE") || typ.equalsIgnoreCase("CHANGE") || typ.equalsIgnoreCase("DECLINE") || typ.equalsIgnoreCase("ACCEPT")) {
				transJson.put("offerid", inputTran.getInt("offerId"));
			}
			transArray.put(transJson);

		}
		data.put("offers", transArray);

		Login.login(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString(), ComunioDataUpdater.community, user); // Stelle gültiges Token sicher
		try {
			Response doc = Jsoup.connect(Urls.COM_API(user) + "/offers")//
					.ignoreContentType(true)//
					.userAgent(HttpHeaderUtil.getRandomUserAgent()) // oder fester User-Agent wie im Browser
					.header("Accept", "application/json, text/plain, */*")//
					.header("Content-Type", "application/json;charset=UTF-8")//
					.header("Authorization", "Bearer " + Login.getToken())//
					.header("Origin", "https://www.comunio.de")//
					.header("Referer", "https://www.comunio.de/exchangemarket")//
					.header("DNT", "1")//
					.header("x-timezone", "Europe/Berlin")//
					.method(Connection.Method.POST)//
					.requestBody(data.toString())//
					.execute();

			JSONObject jResult = new JSONObject(doc.body());
			result = jResult.get("status").equals("OK") ? true : false;
		} catch (IOException e) {
			LOGGER.severe("IOException beim Übermitteln der Transfers: " + e.getMessage());
			LOGGER.throwing(Transfermarkt.class.getName(), "submitTransferForPlayer", e);
			if (e.getMessage() != null && e.getMessage().contains("500")) {
				result = true;
			} else {
				result = false;
			}
		}

		return result;
	}

}
