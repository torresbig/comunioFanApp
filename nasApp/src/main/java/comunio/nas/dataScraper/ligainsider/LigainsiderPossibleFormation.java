package comunio.nas.dataScraper.ligainsider;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.enu.PlayerMatchStatus;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.LigainsiderClubObject;
import comunio.nas.objects.ligainsider.PossibleFormationPlayerObject;
import comunio.nas.util.player.PlayerMatcher;

public class LigainsiderPossibleFormation {

	private static final Logger LOGGER = LogManager.getLogger(LigainsiderPossibleFormation.class);

	private Map<String, Set<PossibleFormationPlayerObject>> possibleFormationMap;
	private Instant lastUpdate;
	private Integer matchday;
	
	

	public LigainsiderPossibleFormation(JSONObject json) {
		this.possibleFormationMap = new HashMap<>();
		if (json != null) {
			if (json.has("lastUpdate") && !json.isNull("lastUpdate")) {
				this.lastUpdate = Instant.parse(json.getString("lastUpdate"));
			}

			if (json.has("matchday") && !json.isNull("matchday")) {
				this.setMatchday(json.getInt("matchday"));
			}

			if (json.has("possibleFormationMap") && !json.isNull("possibleFormationMap")) {
				JSONObject mapJson = json.getJSONObject("possibleFormationMap");
				for (String clubId : mapJson.keySet()) {
					JSONArray playersArray = mapJson.getJSONArray(clubId);
					Set<PossibleFormationPlayerObject> playerSet = new HashSet<>();
					for (int i = 0; i < playersArray.length(); i++) {
						JSONObject playerJson = playersArray.getJSONObject(i);
						playerSet.add(PossibleFormationPlayerObject.fromJSON(playerJson));
					}
					this.possibleFormationMap.put(clubId, playerSet);
				}
			}
		}
	}

	public static LigainsiderPossibleFormation fromJSON(JSONObject json) {
		return new LigainsiderPossibleFormation(json);
	}

	public JSONObject toJSON() {
		JSONObject result = new JSONObject();

		if (this.lastUpdate != null) {
			result.put("lastUpdate", this.lastUpdate.toString());
		} else {
			result.put("lastUpdate", JSONObject.NULL);
		}
		if (this.matchday != null) {
			result.put("matchday", this.matchday);
		} else {
			result.put("matchday", JSONObject.NULL);
		}

		JSONObject mapJson = new JSONObject();
		if (this.possibleFormationMap != null) {
			for (Map.Entry<String, Set<PossibleFormationPlayerObject>> entry : this.possibleFormationMap.entrySet()) {
				JSONArray playersArray = new JSONArray();
				if (entry.getValue() != null) {
					for (PossibleFormationPlayerObject player : entry.getValue()) {
						playersArray.put(player.toJSON());
					}
				}
				mapJson.put(entry.getKey(), playersArray);
			}
		}
		result.put("possibleFormationMap", mapJson);

		return result;
	}

	public Map<String, Set<PossibleFormationPlayerObject>> updatePossibleFormation(Map<String, ClubObject> clubDb, JSONObject playerDbObject, MatchdayInfo currentMatchdayInfo) {
		if (this.possibleFormationMap == null) {
			this.possibleFormationMap = new HashMap<>();
		}
		
		if(!currentMatchdayInfo.canFetchForPossibleFormation()) {
			return this.possibleFormationMap; 
		}
		this.possibleFormationMap.clear();
		
		this.matchday = currentMatchdayInfo.getPointsMatchday();
		try {
			if (clubDb != null && !clubDb.isEmpty()) {
				for (Map.Entry<String, ClubObject> entry : clubDb.entrySet()) {
					ClubObject club = entry.getValue();
					if (club.isInLiga() && club.getLigainsiderClub() != null && club.getLigainsiderClub().getLink() != null && !club.getLigainsiderClub().getLink().isBlank()) {
						
						String clubId = club.getId();
						String clubName = club.getName();
						
						Set<PossibleFormationPlayerObject> possibleFormationClubSet = this.possibleFormationMap.containsKey(clubId) ? this.possibleFormationMap.get(clubId) : new HashSet<>() ;
						
						parseLigainsiderFormations(club, possibleFormationClubSet);
						
						if (!possibleFormationClubSet.isEmpty()) {
							
							possibleFormationClubSet.forEach(p -> {
								JSONObject player = PlayerMatcher.findPlayerByNameAndClub(playerDbObject.getJSONArray("playerDB"), clubDb, p.getPlayerName(), clubName);
								if(player == null) {
									player = PlayerMatcher.findPlayerByNameAndClub(playerDbObject.getJSONArray("playerDB"), clubDb, p.extractPlayerNameFromUrl(), clubName);
								}
								if (player != null && !player.isEmpty()) {
									p.setComunioPlayerId(player.getString("id"));
									JSONObject data = player.getJSONObject("data");
									data.put("ligainsider", p.toPlayerDataJSON());
								} else {
									ComunioDataUpdater.errorDb.addError(new Error(ErrorType.LIGAINSIDER_POSSIBLE_FORMATION, "Spieler wurde nicht gefunden! Spieler" + p.getPlayerName() + " | Club: " + clubName));
								}
							});
						}
						
						this.possibleFormationMap.put(clubId, possibleFormationClubSet);
						this.setLastUpdate(Instant.now());
						
					} else {
						LOGGER.warning("Club hat keine daten von Ligainsider! Club: " + club.toString());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warning("Irgendwas ist schief gelaufen: " + e.getMessage());
		}
		return this.possibleFormationMap;
	}

	private void parseLigainsiderFormations(ClubObject club, Set<PossibleFormationPlayerObject> possibleFormationClubSet) {

		if (club == null || club.getLigainsiderClub() == null) {
			return;
		}
		LigainsiderClubObject lco = club.getLigainsiderClub();
		try {
			// Verbindung aufbauen und Response abfangen
			Connection.Response response = Jsoup.connect(lco.getLink()).userAgent("Mozilla/5.0").execute();

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
					int pos = 1;
					for (Element span : colorSpans) {
						String colorClass = span.className(); // z.B. "green", "grey", "orange"
						// green= Startelf yellow= eingewechselt orange = bank red= nicht im kadeer

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

					PossibleFormationPlayerObject pfpo = new PossibleFormationPlayerObject(playerName, playerId, lco.getId(), playerLink, playerNote, last5MatchStatus);
					updateOrInsert(possibleFormationClubSet, pfpo);

				}
			} else {
				System.out.println("Fehler: Server antwortete mit Status Code " + response.statusCode());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateOrInsert(Set<PossibleFormationPlayerObject> possibleFormationClubSet, PossibleFormationPlayerObject pfpo) {
		for (PossibleFormationPlayerObject existing : possibleFormationClubSet) {
			if (existing.getLigainsiderPlayerId().equals(pfpo.getLigainsiderPlayerId())) {
				possibleFormationClubSet.remove(existing);
				break;
			}
		}
		possibleFormationClubSet.add(pfpo);
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public Map<String, Set<PossibleFormationPlayerObject>> getPossibleFormationMap() {
		return possibleFormationMap;
	}

	public void setPossibleFormationMap(Map<String, Set<PossibleFormationPlayerObject>> possibleFormationMap) {
		this.possibleFormationMap = possibleFormationMap;
	}

	public int getMatchday() {
		return matchday;
	}

	public void setMatchday(int matchday) {
		this.matchday = matchday;
	}

}
