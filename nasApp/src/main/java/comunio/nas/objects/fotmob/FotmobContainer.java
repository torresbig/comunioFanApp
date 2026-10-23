package comunio.nas.objects.fotmob;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.ClubMapper;
import comunio.nas.util.player.PlayerMatcher;

public class FotmobContainer {

	private static final Logger LOGGER = LogManager.getLogger(FotmobContainer.class);

	private Map<String, FotmobPlayerDataObject> playerMap; // key muss die ComunioPlayer ID sein! fotmob id ist in der Klasse.
	private Map<String, FotmobClubDataObject> clubMap; // key muss die ComunioClub ID sein! fotmob id ist in der Klasse.
	private Instant lastUpdate;

	public FotmobContainer() {
		this.playerMap = new HashMap<>();
		this.clubMap = new HashMap<>();
	}
	


	public Map<String, FotmobPlayerDataObject> getPlayerMap() {
		return playerMap;
	}

	public void setPlayerMap(Map<String, FotmobPlayerDataObject> playerMap) {
		this.playerMap = playerMap;
	}

	public Map<String, FotmobClubDataObject> getClubMap() {
		return clubMap;
	}

	public void setClubMap(Map<String, FotmobClubDataObject> clubMap) {
		this.clubMap = clubMap;
	}

	public void setLastUpdate(Instant now) {
		this.lastUpdate = now;
	}

	public Instant getLastUpdate() {
		return this.lastUpdate;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		if (lastUpdate != null)
			json.put("lastUpdate", lastUpdate.toString());

		// clubMap serialisieren
		JSONObject clubMapJson = new JSONObject();
		if (clubMap != null) {
			for (Map.Entry<String, FotmobClubDataObject> entry : clubMap.entrySet()) {
				clubMapJson.put(entry.getKey(), entry.getValue().toJSON());
			}
		}
		json.put("clubMap", clubMapJson);

		// playerMap serialisieren
		JSONObject playerMapJson = new JSONObject();
		if (playerMap != null) {
			for (Map.Entry<String, FotmobPlayerDataObject> entry : playerMap.entrySet()) {
				playerMapJson.put(entry.getKey(), entry.getValue().toJSON());
			}
		}
		json.put("playerMap", playerMapJson);

		return json;
	}

	public static FotmobContainer fromJson(JSONObject fobmobMapping) {
		FotmobContainer container = new FotmobContainer();

		if (fobmobMapping.has("lastUpdate")) {
			container.setLastUpdate(Instant.parse(fobmobMapping.getString("lastUpdate")));
		}

		// clubMap deserialisieren
		if (fobmobMapping.has("clubMap")) {
			JSONObject clubMapJson = fobmobMapping.optJSONObject("clubMap");
			if (clubMapJson != null) {
				Map<String, FotmobClubDataObject> clubMap = new HashMap<>();
				for (String key : clubMapJson.keySet()) {
					JSONObject clubJson = clubMapJson.optJSONObject(key);
					if (clubJson != null) {
						clubMap.put(key, FotmobClubDataObject.fromJSON(clubJson));
					}
				}
				container.setClubMap(clubMap);
			}
		}

		// playerMap deserialisieren
		if (fobmobMapping.has("playerMap")) {
			JSONObject playerMapJson = fobmobMapping.optJSONObject("playerMap");
			if (playerMapJson != null) {
				Map<String, FotmobPlayerDataObject> playerMap = new HashMap<>();
				for (String key : playerMapJson.keySet()) {
					JSONObject playerJson = playerMapJson.optJSONObject(key);
					if (playerJson != null) {
						playerMap.put(key, FotmobPlayerDataObject.fromJSON(playerJson));
					}
				}
				container.setPlayerMap(playerMap);
			}
		}

		return container;
	}

	/**
	 * Parses FotMob player data for the given player ID.
	 *
	 * @param playerId the player ID as a string
	 * @return parsed FotmobPlayerDataObject or null if parsing fails
	 */
	public static FotmobPlayerDataObject parseFotmobPlayerData(String playerId) {
		String url = String.format(Urls.PLAYER_DATA_URL, playerId);
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
		int MAX_RETRIES = 3;
		Duration RETRY_DELAY = Duration.ofSeconds(2);
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

	/**
	 * Generic method to fetch JSON from a URL with retry logic.
	 *
	 * @param url the URL to fetch
	 * @return JSONObject or null if all retries fail
	 */
	private static JSONObject fetchLigaTeamsJsonWithRetry(String url) {
		int MAX_RETRIES = 3;
		Duration RETRY_DELAY = Duration.ofSeconds(2);
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				LOGGER.log(Level.FINE, "Fetching JSON from: " + url + " (Attempt " + attempt + "/" + MAX_RETRIES + ")");

				Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36").ignoreContentType(true).timeout(10000).get();

				String responseBody = doc.body().text();
				if (responseBody != null && !responseBody.isEmpty()) {
					JSONArray resArr = new JSONArray(responseBody);
					for (int i = 0; i < resArr.length(); i++) {
						JSONObject obj = resArr.optJSONObject(i);
						if (obj != null && obj.has("data")) {
							return obj;
						}
					}
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

	/**
	 * Generic method to fetch JSON from a URL with retry logic.
	 *
	 * @param url the URL to fetch
	 * @return JSONObject or null if all retries fail
	 */
	private static JSONObject fetchJsonWithRetry(String url) {
		int MAX_RETRIES = 3;
		Duration RETRY_DELAY = Duration.ofSeconds(2);
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				LOGGER.log(Level.FINE, "Fetching JSON from: " + url + " (Attempt " + attempt + "/" + MAX_RETRIES + ")");

				Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36").ignoreContentType(true).timeout(10000).get();

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

	/**
	 * Fetches the league table from FotMob for the given league ID.
	 *
	 * @return JSONArray of clubs or null if fetching fails
	 */
	public static JSONArray fetchLeagueTable() {
		JSONObject json = fetchLigaTeamsJsonWithRetry(Urls.LEAGUE_TABLE_URL);
		if (json == null) {
			LOGGER.warning("Failed to fetch league table from: " + Urls.LEAGUE_TABLE_URL);
			return null;
		}
		try {
			JSONObject dataObj = json.optJSONObject("data");
			if (dataObj != null) {
				JSONObject tableInner = dataObj.optJSONObject("table");
				if (tableInner != null) {
					return tableInner.optJSONArray("all");
				}
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error parsing league table JSON", e);
			e.getMessage();
		}
		return null;
	}

	/**
	 * Parses the league table JSONArray into a list of FotmobClubDataObject. Sets
	 * the comunioId using ClubMapper.getComunioIdFromName.
	 *
	 * @param clubsArray The JSONArray of clubs from the league table
	 * @param clubDb     Map of ClubObject with comunio IDs as keys
	 * @return Map<String, FotmobClubDataObject> with fotmob club id as key
	 */
	public Map<String, FotmobClubDataObject> parseClubsFromTable(JSONArray clubsArray, Map<String, ClubObject> clubDb) {
		if (this.clubMap == null) {
			clubMap = new HashMap<>();
		}
		if (clubsArray == null) {
			LOGGER.warning("Clubs array is null");
			return clubMap;
		}
		StringBuilder debug = new StringBuilder(); 
		for (int i = 0; i < clubsArray.length(); i++) {
			JSONObject clubJson = clubsArray.optJSONObject(i);
			if (clubJson == null)
				continue;

			String fotmobId = String.valueOf(clubJson.optInt("id"));
			String name = clubJson.optString("name");
			String pageUrl = clubJson.optString("pageUrl");

			FotmobClubDataObject club = new FotmobClubDataObject();
			club.setId(fotmobId);
			club.setName(name);
			club.setLink("https://www.fotmob.com" + pageUrl);
			club.setLastUpdate(Instant.now());


			String comunioId = ClubMapper.getComunioIdFromName(name, clubDb);
			if (comunioId == null || comunioId.isBlank()) {
				debug.append("No Comunio ID found for club: " + name + " (FotMob ID: " + fotmobId + ")\n");
				continue;
			}
			club.setComunioId(comunioId);

			clubMap.put(comunioId, club);
		}
		debug.append("Parsed " + clubMap.size() + " clubs from league table\n");
		debug.append(clubMap.values().stream().map(c -> c.getName() + " (FotMob ID: " + c.getId() + ", Comunio ID: " + c.getComunioId() + ")").reduce("", (a, b) -> a + "\n" + b));
		LOGGER.info(debug.toString());
		return clubMap;
	}

	/**
	 * Builds a FotmobContainer with the clubMap filled from the league table.
	 *
	 * @param clubDb Map of ClubObject with comunio IDs as keys
	 * @return FotmobContainer with clubMap populated, or null if fetching fails
	 */
	public FotmobContainer buildFotmobContainer(Map<String, ClubObject> clubDb) {
		JSONArray clubsArray = fetchLeagueTable();
		if (clubsArray == null) {
			LOGGER.severe("Could not fetch league table");
			return null;
		}

		if (this.clubMap != null && this.clubMap.size() == 18) {
			return this;
		}

		this.clubMap = parseClubsFromTable(clubsArray, clubDb);
		if (clubMap.size() < 18) {
			LOGGER.warning("Expected 18 clubs, got " + clubMap.size());
		}

		this.setLastUpdate(Instant.now());
		return this;
	}

	/**
	 * Fetches the squad data for a specific club by its FotMob ID.
	 *
	 * @param clubId The FotMob club ID
	 * @return JSONObject containing the squad data or null if fetching fails
	 */
	public static JSONObject fetchClubSquad(String clubId) {
		String url = String.format(Urls.CLUB_SQUAD_URL, clubId);
		JSONObject json = fetchJsonWithRetry(url);
		if (json == null) {
			LOGGER.warning("Failed to fetch club squad for ID: " + clubId);
			return null;
		}
		return json;
	}

	/**
	 * Converts a squad member JSONObject from the club squad API into a
	 * FotmobPlayerDataObject. This maps the simplified club data to the internal
	 * data structure.
	 *
	 * @param member The JSONObject representing a squad member
	 * @return FotmobPlayerDataObject with mapped data
	 */
	public static FotmobPlayerDataObject convertSquadMemberToPlayerDataObject(JSONObject member) {
		if (member == null)
			return null;

		FotmobPlayerDataObject player = new FotmobPlayerDataObject();

		// Basic fields
		player.setId(member.optInt("id"));
		player.setName(member.optString("name"));
		player.setStatus(member.optString("status"));

		// Captain status from role
		JSONObject role = member.optJSONObject("role");
		if (role != null) {
			String roleKey = role.optString("key");
			player.setIsCaptain("captain_long".equals(roleKey) || "captain_short".equals(roleKey));
		}

		// Injury information
		if (member.has("injured") && member.optBoolean("injured")) {
			InjuryInformation injury = new InjuryInformation();
			JSONObject injuryObj = member.optJSONObject("injury");
			if (injuryObj != null) {
				injury.setName(injuryObj.optString("name"));
				injury.setExpectedReturn(injuryObj.optString("expectedReturn"));
			} else {
				injury.setName("Verletzt");
			}
			player.setInjuryInformation(injury);
		}

		// Position details
		List<PositionDetail> positions = new ArrayList<>();
		PositionDetail pos = new PositionDetail();

		// Use role fallback as position name
		if (role != null) {
			pos.setStrPos(role.optString("fallback"));
		}

		// Use positionIdsDesc as short position
		String posIdsDesc = member.optString("positionIdsDesc");
		if (posIdsDesc != null && !posIdsDesc.isEmpty()) {
			pos.setStrPosShort(posIdsDesc.split(",")[0].trim());
		}

		pos.setIsMainPosition(true);
		positions.add(pos);
		player.setPositions(positions);

		// Player information
		List<PlayerInformation> playerInfoList = new ArrayList<>();

		// Height
		if (member.has("height") && !member.isNull("height")) {
			PlayerInformation heightInfo = new PlayerInformation();
			heightInfo.setTitle("Height");
			PlayerInfoValue heightValue = new PlayerInfoValue();
			heightValue.setNumberValue(member.optInt("height"));
			heightValue.setFallback(member.optInt("height") + " cm");
			heightInfo.setValue(heightValue);
			playerInfoList.add(heightInfo);
		}

		// Shirt
		if (member.has("shirtNumber") && !member.isNull("shirtNumber")) {
			PlayerInformation shirtInfo = new PlayerInformation();
			shirtInfo.setTitle("Shirt");
			PlayerInfoValue shirtValue = new PlayerInfoValue();
			shirtValue.setNumberValue(member.optInt("shirtNumber"));
			shirtValue.setFallback(String.valueOf(member.optInt("shirtNumber")));
			shirtInfo.setValue(shirtValue);
			playerInfoList.add(shirtInfo);
		}

		// Age
		if (member.has("age") && !member.isNull("age")) {
			PlayerInformation ageInfo = new PlayerInformation();
			ageInfo.setTitle("Age");
			PlayerInfoValue ageValue = new PlayerInfoValue();
			ageValue.setNumberValue(member.optInt("age"));
			ageValue.setFallback(String.valueOf(member.optInt("age")));
			ageInfo.setValue(ageValue);
			playerInfoList.add(ageInfo);
		}

		// Country
		String cname = member.optString("country");
		if (cname != null && !cname.isEmpty()) {
			PlayerInformation countryInfo = new PlayerInformation();
			countryInfo.setTitle("Country");
			PlayerInfoValue countryValue = new PlayerInfoValue();
			countryValue.setFallback(cname);
			countryInfo.setValue(countryValue);
			playerInfoList.add(countryInfo);
		}

		// Market value
		if (member.has("transferValue") && !member.isNull("transferValue")) {
			PlayerInformation valueInfo = new PlayerInformation();
			valueInfo.setTitle("Market value");
			PlayerInfoValue valueValue = new PlayerInfoValue();
			int transferValue = member.optInt("transferValue");
			valueValue.setNumberValue(transferValue);
			valueValue.setFallback("€" + transferValue);
			valueInfo.setValue(valueValue);
			playerInfoList.add(valueInfo);
		}

		// Contract end (from dateOfBirth? Actually contract end not in squad, but we
		// can add dateOfBirth)
		if (member.has("dateOfBirth") && !member.isNull("dateOfBirth")) {
			PlayerInformation dobInfo = new PlayerInformation();
			dobInfo.setTitle("Date of Birth");
			PlayerInfoValue dobValue = new PlayerInfoValue();
			dobValue.setDateValue(member.optString("dateOfBirth"));
			dobValue.setFallback(member.optString("dateOfBirth"));
			dobInfo.setValue(dobValue);
			playerInfoList.add(dobInfo);
		}

		player.setPlayerInformation(playerInfoList);

		return player;
	}

	/**
	 * Checks if debug mode is enabled via ComunioDataUpdater.uld. Falls back to
	 * false if uld is not available.
	 *
	 * @return true if debug mode is active, false otherwise
	 */
	private boolean isDebugMode() {
		try {
			if (ComunioDataUpdater.uld != null) {
				return ComunioDataUpdater.uld.isDebug();
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not check debug mode", e);
		}
		return false;
	}

	/**
	 * Fetches squad data for all clubs in the clubMap and fills the playerMap. This
	 * uses the fast club squad API (teams?id=...) for each club. A 2-second timeout
	 * between requests is added unless debug mode is active.
	 *
	 * @param clubDb Map of ClubObject with comunio IDs as keys (needed if clubMap
	 *               is empty)
	 * @return this FotmobContainer with filled playerMap, or null on critical
	 *         failure
	 */
	public void fetchAndFillPlayerMap(Map<String, ClubObject> clubDb, JSONArray playerDB) {
		LOGGER.info("Starting playerMap fetch for all clubs...");
		int successClubs = 0;
		int failClubs = 0;
		int totalPlayers = 0;

		// Ensure clubMap is populated
		if (this.clubMap == null || this.clubMap.isEmpty() || this.clubMap.size() < 18) {
			LOGGER.warning("clubMap is empty, building it first...");
			if (clubDb != null) {
				this.buildFotmobContainer(clubDb);
			} else {
				LOGGER.severe("Cannot fetch players: clubDb is null and clubMap is empty");
				return;
			}
		}

		// Convert to List for safe iteration
		int totalClubs = clubMap.values().size();

		int index = 0;
		for (Map.Entry<String, FotmobClubDataObject> entry : clubMap.entrySet()) {
			String clubId = entry.getValue().getId();
			FotmobClubDataObject club = entry.getValue();

			LOGGER.fine("Fetching squad for club: " + club.getName() + " (ID: " + clubId + ")");

			JSONObject squadJson = fetchClubSquad(clubId);
			if (squadJson == null) {
				LOGGER.warning("Failed to fetch squad for club: " + club.getName());
				failClubs++;
				// Timeout before next request even on failure
				if (index < totalClubs - 1 && !isDebugMode()) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						LOGGER.log(Level.SEVERE, "Thread interrupted during timeout - " + ie);
						break;
					}
				}
				continue;
			}

			// Parse squad structure: squad.squad is an array of groups (keepers, defenders,
			// etc.)
			JSONObject squadObj = squadJson.optJSONObject("squad");
			if (squadObj == null) {
				LOGGER.warning("No squad object in response for club: " + club.getName());
				failClubs++;
				if (index < totalClubs - 1 && !isDebugMode()) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						LOGGER.log(Level.SEVERE, "Thread interrupted during timeout - " + ie);
						break;
					}
				}
				continue;
			}

			JSONArray squadGroups = squadObj.optJSONArray("squad");
			if (squadGroups == null) {
				LOGGER.warning("No squad groups in response for club: " + club.getName());
				failClubs++;
				if (index < totalClubs - 1 && !isDebugMode()) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						LOGGER.log(Level.SEVERE, "Thread interrupted during timeout - " + ie);
						break;
					}
				}
				continue;
			}

			int clubPlayers = 0;
			// Iterate through each group (keepers, defenders, midfielders, attackers)
			for (int g = 0; g < squadGroups.length(); g++) {
				JSONObject group = squadGroups.optJSONObject(g);
				if (group == null)
					continue;

				JSONArray members = group.optJSONArray("members");
				if (members == null)
					continue;

				for (int m = 0; m < members.length(); m++) {
					JSONObject memberJson = members.optJSONObject(m);
					if (memberJson == null)
						continue;

					FotmobPlayerDataObject player = convertSquadMemberToPlayerDataObject(memberJson);
					if(player != null && !player.getPositions().isEmpty() && player.getPositions().get(0).getStrPos().equals("Coach")) {
						continue;
					}
					JSONObject comPlayer = PlayerMatcher.findPlayerByNameAndClub(playerDB, clubDb, club.getName(), player.getName());
					if (comPlayer == null || comPlayer.isEmpty()) {
						LOGGER.warning("No matching Comunio player found for Fotmob player: " + player.getName() + " in club: " + club.getName());
						continue;
					}
					String comId = comPlayer.optString("id");
					if (comId == null || comId.isBlank()) {
						LOGGER.warning("No Comunio player has no ID!!!!: " + player.getName() + " in club: " + club.getName());
						continue;
					}

					if (player != null) {
						player.setComunioId(comId);
						player.setClubId(club.getId());
						player.setComunioClubId(club.getComunioId());
						this.playerMap.put(comId, player);
						clubPlayers++;
						totalPlayers++;
					}
				}
			}

			LOGGER.fine("Fetched " + clubPlayers + " players for club: " + club.getName());
			if (clubPlayers > 0) {
				successClubs++;
			} else {
				failClubs++;
			}

			// Timeout between clubs to avoid API rate limiting (skip in debug mode)
			if (index < totalClubs - 1 && !isDebugMode()) {
				LOGGER.fine("Waiting 2 seconds before next club...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					LOGGER.log(Level.SEVERE, "Thread interrupted during timeout - " + ie);
					break;
				}
			}

			index++;
		}

		this.setLastUpdate(Instant.now());
		LOGGER.info("PlayerMap fetch completed: " + successClubs + " clubs successful, " + failClubs + " failed, " + totalPlayers + " total players");
	}

	/**
	 * Updates only players whose lastUpdate is older than maxAgeDays. Per call,
	 * only maxPlayers are fetched and merged into the existing playerMap. This
	 * avoids hitting the API for all players every run.
	 *
	 * @param maxPlayers Maximum number of players to fetch per call (e.g. 5-10)
	 * @param maxAgeDays Maximum age in days before a player is considered stale
	 * @return number of players actually updated, or -1 on critical failure
	 */
	public int updateStalePlayers(int maxPlayers, int maxAgeDays) {
		LOGGER.info("Starting stale player update: max " + maxPlayers + " players, older than " + maxAgeDays + " days");

		if (this.playerMap == null || this.playerMap.isEmpty()) {
			LOGGER.warning("playerMap is empty - nothing to update");
			return 0;
		}

		Instant cutoff = Instant.now().minusSeconds(maxAgeDays * 86400L);
		int updated = 0;
		int checked = 0;

		for (Map.Entry<String, FotmobPlayerDataObject> entry : this.playerMap.entrySet()) {
			if (updated >= maxPlayers) {
				LOGGER.info("Reached maxPlayers limit (" + maxPlayers + "), stopping");
				break;
			}
			checked++;

			FotmobPlayerDataObject existing = entry.getValue();
			if (existing.getLastUpdate() != null && existing.getLastUpdate().isAfter(cutoff)) {
				continue; // Noch aktuell
			}

			LOGGER.fine("Fetching fresh data for stale player: " + entry.getKey());
			String fotmobId = String.valueOf(entry.getValue().getId()); 

			// Fetch fresh data from the long player API
			FotmobPlayerDataObject fresh = parseFotmobPlayerData(fotmobId);
			if (fresh == null) {
				LOGGER.warning("Could not fetch fresh data for player: ComunioId" + entry.getKey() + " - FotmobID: " + fotmobId);
				continue;
			}

			fresh.setLastUpdate(Instant.now());
			this.playerMap.put(entry.getKey(), fresh);
			updated++;

			// Small delay to be gentle on the API
			if (updated < maxPlayers) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					LOGGER.log(Level.SEVERE, "Thread interrupted during update delay - " + ie);
					break;
				}
			}
		}

		LOGGER.info("Stale player update completed: " + updated + " updated, " + checked + " checked");
		return updated;
	}
}