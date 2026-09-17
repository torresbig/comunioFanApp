package comunio.nas.dataScraper.ligainsider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.enu.PlayerMatchStatus;
import comunio.nas.objects.ligainsider.PossibleFormationPlayerObject;

public class LigainsiderPossibleFormation {
	
	 public static void main(String[] args) {
		 parseLigainsiderFormations("3", "eintracht-frankfurt");
	 }
	
	 public static Map<String, PossibleFormationPlayerObject> parseLigainsiderFormations(String clubId, String clubLinkName) {
	        String url = "https://www.ligainsider.de/" + clubLinkName + "/" + clubId; // z.B. die Team-Seite
	        Map<String, PossibleFormationPlayerObject> result = new HashMap<>();
	        try {
	            // Verbindung aufbauen und Response abfangen
	            Connection.Response response = Jsoup.connect(url)
	                    .userAgent("Mozilla/5.0")
	                    .execute();

	            if (response.statusCode() == 200) {
	                Document doc = response.parse();
	                
	                // Alle Spieler-Container finden
	                Elements players = doc.select(".player_position_column");

	                for (Element player : players) {
	                    // 1. Name & Link
	                    Element nameElement = player.selectFirst(".player_name a");
	                    String playerName = (nameElement != null) ? nameElement.text() : "N/A";
	                    String playerLink = (nameElement != null) ? nameElement.absUrl("href") : "N/A";
	                    String playerId = null;
	                    if (playerLink.contains("_")) {
							// Extrahiert "1812" aus "/emre-can_1812/"
							playerId = playerLink.substring(playerLink.lastIndexOf("_") + 1, playerLink.lastIndexOf("/"));
						}

	                    // 2. Note (Extrahiert aus .tags_info span)
	                    Element noteElement = player.selectFirst(".tags_info span");
	                    String playerNote = (noteElement != null) ? noteElement.text().replace("\n", "").trim() : "N/A";

	                    // 3. Farben der letzten 5 Spiele
	                    Elements colorSpans = player.select(".color_box span");
	                    Map<Integer, PlayerMatchStatus> last5MatchStatus = new HashMap<>();
	                    for (Element span : colorSpans) {
	                        String colorClass = span.className(); // z.B. "green", "grey", "orange"
	                        // green= Startelf yellow= eingewechselt orange = bank red= nicht im kadeer
	                        int pos = 1;
	                        switch (colorClass) {
							case "green":
								last5MatchStatus.put(pos, PlayerMatchStatus.START);
								pos++;
								break;
							case "yellow":
								last5MatchStatus.put(pos, PlayerMatchStatus.SUBIN);
								pos++;
								break;
							case "orange":
								last5MatchStatus.put(pos, PlayerMatchStatus.BANK);
								pos++;
								break;
							case "red":
								last5MatchStatus.put(pos, PlayerMatchStatus.NOTINTEAM);
								pos++;
								break;
							default:
								last5MatchStatus.put(pos, PlayerMatchStatus.NONE);
								pos++;
							}
 
	                    }

	                    PossibleFormationPlayerObject pfpo = new PossibleFormationPlayerObject(playerName, playerId, clubId, playerLink, playerNote, last5MatchStatus);
	                    result.put(clubId, pfpo);
	                    
	                }
	            } else {
	                System.out.println("Fehler: Server antwortete mit Status Code " + response.statusCode());
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	

}
