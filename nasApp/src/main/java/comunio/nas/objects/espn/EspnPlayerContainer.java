package comunio.nas.objects.espn;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class EspnPlayerContainer {

	private Map<String, EspnPlayerMapObject> playerMap;
	private int mapped;
	private int mappingFailed;
	private int mappedOnlyLastName;
	private int mappedInitials;
	private Set<EspnPlayerMapObject> failedPlayers;

	public EspnPlayerContainer(Map<String, EspnPlayerMapObject> playerMap) {
		this.setPlayerMap(playerMap);
		this.setMapped();
		this.setFailedPlayers(new HashSet<>());
		this.setMappingFailed();
	}

	public Map<String, EspnPlayerMapObject> getPlayerMap() {
		return playerMap;
	}

	public void setPlayerMap(Map<String, EspnPlayerMapObject> playerMap) {
		this.playerMap = playerMap;
	}

	public int getMapped() {
		return mapped;
	}

	/**
	 * Setzt die Anzahl der erfolgreich gemappten Spieler basierend auf der Größe der playerMap.
	 */
	public void setMapped() {
		if (this.playerMap != null) {
			this.mapped = this.playerMap.size();
		} else {
			this.mapped = 0;
		}
	}

	public int getMappingFailed() {
		return mappingFailed;
	}

	
	/**
	 * Setzt die Anzahl der fehlgeschlagenen Mappings basierend auf der Größe der failedPlayers-Menge.
	 */
	private void setMappingFailed() {
		if (this.failedPlayers != null) {
			this.mappingFailed = this.failedPlayers.size();
		} else {
			this.mappingFailed = 0;
		}
	}

	public Set<EspnPlayerMapObject> getFailedPlayers() {
		return failedPlayers;
	}

	public void setFailedPlayers(Set<EspnPlayerMapObject> failedPlayers) {
		this.failedPlayers = failedPlayers;
		this.setMappingFailed(); // Aktualisiert die Anzahl der fehlgeschlagenen Mappings basierend auf der neuen Menge
	}
	
	/**
	 * Fügt einen fehlgeschlagenen Spieler zur failedPlayers-Menge hinzu.
	 *
	 * @param failedPlayer Das EspnPlayerMapObject des fehlgeschlagenen Spielers.
	 */
	public void addFailedPlayer(EspnPlayerMapObject failedPlayer) {
		if (this.failedPlayers == null) {
			this.failedPlayers = new HashSet<>();
		}
		this.failedPlayers.add(failedPlayer);
		this.setMappingFailed(); // Aktualisiert die Anzahl der fehlgeschlagenen Mappings basierend auf der neuen Menge
	}
	
	/**
	 * Entfernt einen fehlgeschlagenen Spieler aus der failedPlayers-Menge basierend auf der ESPN-ID.
	 *
	 * @param espnId Die ESPN-ID des Spielers, der entfernt werden soll.
	 */
	public void removeFailedPlayer(String espnId) {
		if (this.failedPlayers != null) {
			this.failedPlayers.removeIf(player -> player.getEspnId().equals(espnId));
		}
		
	}
	
	

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		JSONArray playerMapJson = new JSONArray();
		for (Map.Entry<String, EspnPlayerMapObject> entry : this.playerMap.entrySet()) {
			EspnPlayerMapObject player = entry.getValue();			
			playerMapJson.put(player.toJson());
		}
		json.put("mapping", playerMapJson);
		json.put("mapped", mapped);
		json.put("mappingFailed", mappingFailed);
		json.put("mappedOnlyLastName", mappedOnlyLastName);
		json.put("mappedInitials", mappedInitials);
		for (EspnPlayerMapObject failedPlayer : failedPlayers) {
			json.append("failedPlayers", failedPlayer.toJson());
		}
		
		return json;
	}
	
	public static EspnPlayerContainer fromJson(JSONObject json) {
		Map<String, EspnPlayerMapObject> playerMap = new java.util.HashMap<>();
		JSONArray playerMapJson = json.optJSONArray("mapping", new JSONArray());
		for (int i = 0; i < playerMapJson.length(); i++) {
			JSONObject playerJson = playerMapJson.getJSONObject(i);
			EspnPlayerMapObject player = new EspnPlayerMapObject();
			player = EspnPlayerMapObject.fromJson(playerJson);
			playerMap.put(player.getEspnId(), player);
		}
		EspnPlayerContainer container = new EspnPlayerContainer(playerMap);

		if (json.has("failedPlayers")) {
			JSONArray failedPlayersJson = json.optJSONArray("failedPlayers", new JSONArray());
			Set<EspnPlayerMapObject> failedPlayersSet = new HashSet<>();
			for (int i = 0; i < failedPlayersJson.length(); i++) {
				JSONObject failedPlayerJson = failedPlayersJson.getJSONObject(i);
				EspnPlayerMapObject failedPlayer = new EspnPlayerMapObject();
				failedPlayer = EspnPlayerMapObject.fromJson(failedPlayerJson);
				failedPlayersSet.add(failedPlayer);
			}
			container.setFailedPlayers(failedPlayersSet);
		}
		container.setMappedOnlyLastName(json.optInt("mappedOnlyLastName", 0));
		container.setMappedInitials(json.optInt("mappedInitials", 0));
		
		return container;
	}

	public int getMappedOnlyLastName() {
		return mappedOnlyLastName;
	}

	public void setMappedOnlyLastName(int mappedOnlyLastName) {
		this.mappedOnlyLastName = mappedOnlyLastName;
	}

	public int getMappedInitials() {
		return mappedInitials;
	}

	public void setMappedInitials(int mappedInitials) {
		this.mappedInitials = mappedInitials;
	}

}
