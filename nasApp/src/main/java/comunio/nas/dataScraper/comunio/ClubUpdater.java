package comunio.nas.dataScraper.comunio;

import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.HttpHeaderUtil;

public class ClubUpdater {
	private static final Logger LOGGER = LogManager.getLogger(ClubUpdater.class);

	/**
	 * Ruft die Comunio-Clubs dynamisch ab und reichert sie mit den Daten
	 * aus der Transfermarkt-Schnittstelle an.
	 *
	 * @return JSONArray mit JSONObjects im Format:
	 * { "inLiga", "name", "id", "transfermarktDoDe": { "name", "link", "id" } }
	 */
	public static JSONArray fetchClubsAsArray(JSONArray clubDB) {
	    String url = "https://www.comunio.de/api/clubs";

	    try {
	        // 1. Token-Validierung und API-Abruf analog zu deinem Vorbild
	        Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
	        
	        String jsonResponse = Jsoup.connect(url)
	                .userAgent(HttpHeaderUtil.getRandomUserAgent())
	                .header("Accept", "application/json, text/plain, */*")
	                .header("Authorization", "Bearer " + Login.getToken())
	                .header("Accept-Encoding", "gzip, deflate, br, zstd")
	                .header("Accept-Language", "de-DE,en-EN;q=0.9")
	                .header("x-timezone", "Europe/Berlin")
	                .ignoreContentType(true)
	                .execute()
	                .body();

	        JSONObject root = new JSONObject(jsonResponse);

	        if (!root.has("clubs")) {
	            LOGGER.warning("fetchClubsAsArray: Antwort enthält kein 'clubs'-Objekt. URL: " + url);
	            return new JSONArray();
	        }

	        JSONArray clubs = root.optJSONArray("clubs");
	        if (clubs == null) {
	            LOGGER.warning("fetchClubsAsArray: 'clubs' ist null oder fehlt. URL: " + url);
	            return new JSONArray();
	        }

	        StringBuilder log = new StringBuilder();
	        int skipped = 0;

	        // 2. Schleife über alle von Comunio gelieferten Clubs
	        for (int i = 0; i < clubs.length(); i++) {
	            JSONObject clubItem = clubs.optJSONObject(i);
	            if (clubItem == null) {
	                skipped++;
	                log.append("Club-Item an Position ").append(i).append(" ist null – übersprungen.\n");
	                continue;
	            }

	            // ID validieren (Muss vorhanden sein)
	            long clubIdLong = clubItem.optLong("id", -1L);
	            if (clubIdLong <= 0) {
	                skipped++;
	                log.append("Ungültige oder fehlende Club-ID an Position ").append(i).append(" – übersprungen.\n");
	                continue;
	            }
	            
	            String clubId = String.valueOf(clubIdLong);
	            String clubName = clubItem.optString("name", "");
	            String abbreviation = clubItem.optString("abbreviation", null);

	            // Sonderfall abfangen: Wenn Name ungültig/Dummy oder Abkürzung null ist (wie bei "left-league" oder "SC Paderborn" ohne Abkürzung)
	            if (clubName.isEmpty() || "null".equalsIgnoreCase(clubName) || abbreviation == null) {
	            	clubDB.put(createUnknownClubFallback());
	                log.append("Club geliefert, der nicht aktiv in der Liga ist (z.B. ").append(clubName).append("). Als UNBEKANNT gesetzt.\n");
	                continue;
	            }

	            // 3. Dynamischer Abruf der Transfermarkt-Daten für diesen spezifischen Verein
	            // (Ersetze 'TransfermarktAPI.getDetailsForClub' durch deinen tatsächlichen Service/Datenbank-Call)
	           // TODO: noch tmde einfügen um die daten anzureichern.  
//	            JSONObject tmDetails = new JSONObject(); //TransfermarktAPI.getDetailsForClub(clubId, clubName);

	            JSONObject out = new JSONObject();
	            out.put("inLiga", true);
                out.put("name", clubName);
                out.put("id", clubId);
//                TODO: TMDE einfügen um die daten anzureichern.
//	            if (tmDetails != null && tmDetails.has("id") && tmDetails.optInt("id", 0) > 0) {
//	                // Verschachteltes Transfermarkt-Objekt dynamisch bauen
//	                JSONObject outTm = new JSONObject();
//	                outTm.put("name", tmDetails.optString("name", clubName));
//	                outTm.put("link", tmDetails.optString("link", ""));
//	                outTm.put("id", tmDetails.optInt("id", 0));
//	                
//	                out.put("transfermarktDoDe", outTm);
//	            } else {
//	                // Falls die API/DB für diese ID aktuell nichts liefert -> Fallback auf UNBEKANNT
//	                out = createUnknownClubFallback();
//	                log.append("Keine Transfermarkt-Daten dynamisch gefunden für: ").append(clubName).append(" (ID: ").append(clubId).append(")\n");
//	            }

                clubDB.put(out);
	        }

	        if (skipped > 0 || log.length() > 0) {
	            LOGGER.info("fetchClubsAsArray: " + clubDB.length() + " Clubs verarbeitet, " + skipped + " übersprungen.\n" + log.toString());
	        }

	        return clubDB;

	    } catch (Exception e) {
	        LOGGER.warning("fetchClubsAsArray: Fehler beim dynamischen Abrufen/Verarbeiten der Clubs: " + e.toString());
	        return new JSONArray();
	    }
	}

	/**
	 * Hilfsmethode zur Erzeugung des standardisierten UNBEKANNT-Fallbacks
	 */
	private static JSONObject createUnknownClubFallback() {
	    JSONObject out = new JSONObject();
	    out.put("inLiga", false);
	    out.put("name", "UNBEKANNT");
	    out.put("id", "0");

	    JSONObject tmUnknown = new JSONObject();
	    tmUnknown.put("name", "");
	    tmUnknown.put("link", "");
	    tmUnknown.put("id", 0);
	    out.put("transfermarktDoDe", tmUnknown);
	    return out;
	}


}
