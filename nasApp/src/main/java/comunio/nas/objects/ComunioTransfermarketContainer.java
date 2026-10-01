package comunio.nas.objects;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.comunioTransfermarkt.PlayerOnMarket;
import comunio.nas.objects.comunioTransfermarkt.PlayerWasOnMarket;

public class ComunioTransfermarketContainer {

	private Map<String, PlayerOnMarket> transfermarktMap;
	private Map<String, Set<PlayerWasOnMarket>> playerOnMarketCounter;
	private Instant lastUpdate;

	public ComunioTransfermarketContainer() {
		this.setTransfermarktMap(new HashMap<>());
		this.setPlayerOnMarketCounter(new HashMap<>());
		this.setLastUpdate(null);
	}

	public ComunioTransfermarketContainer(Map<String, PlayerOnMarket> transfermarktMap, Map<String, Set<PlayerWasOnMarket>> playerOnMarketCounter, Instant lastUpdate) {
		this.setTransfermarktMap(transfermarktMap);
		this.setPlayerOnMarketCounter(playerOnMarketCounter);
		this.setLastUpdate(lastUpdate);
	}

	public Map<String, PlayerOnMarket> getTransfermarktMap() {
		return transfermarktMap;
	}

	public void setTransfermarktMap(Map<String, PlayerOnMarket> transfermarktMap) {
		this.transfermarktMap = transfermarktMap;
	}

	public Map<String, Set<PlayerWasOnMarket>> getPlayerOnMarketCounter() {
		return playerOnMarketCounter;
	}

	public void setPlayerOnMarketCounter(Map<String, Set<PlayerWasOnMarket>> playerOnMarketCounter) {
		this.playerOnMarketCounter = playerOnMarketCounter;
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * Serialisiert diesen Container in ein {@link JSONObject}. Die inneren Maps
	 * werden in JSON-freundliche Strukturen umgewandelt.
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();

		// transfermarktMap → Map<String, JSONObject>
		JSONObject transfermarktObj = new JSONObject();
		if (transfermarktMap != null) {
			for (Map.Entry<String, PlayerOnMarket> entry : transfermarktMap.entrySet()) {
				transfermarktObj.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toJSON() : JSONObject.NULL);
			}
		}
		json.put("transfermarktMap", transfermarktObj);

		// playerOnMarketCounter → Map<String, JSONArray>
		JSONObject counterObj = new JSONObject();
		if (playerOnMarketCounter != null) {
			for (Map.Entry<String, Set<PlayerWasOnMarket>> entry : playerOnMarketCounter.entrySet()) {
				JSONArray arr = new JSONArray();
				if (entry.getValue() != null) {
					for (PlayerWasOnMarket player : entry.getValue()) {
						arr.put(player != null ? player.toJSON() : JSONObject.NULL);
					}
				}
				counterObj.put(entry.getKey(), arr);
			}
		}
		json.put("playerOnMarketCounter", counterObj);

		// lastUpdate
		json.put("lastUpdate", lastUpdate != null ? lastUpdate.toString() : null);

		return json;
	}

	/**
	 * Deserialisiert einen Container aus einem {@link JSONObject}. Null-sicher:
	 * fehlende oder {@code null}-Felder werden durch leere Maps ersetzt.
	 *
	 * @param json Das JSON-Objekt.
	 * @return Neues {@code ComunioTransfermarketContainer}-Objekt.
	 */
	public static ComunioTransfermarketContainer fromJSON(Object obj) {
		ComunioTransfermarketContainer result = new ComunioTransfermarketContainer();

		if (obj == null) {
			return result;
		}

		Map<String, Set<PlayerWasOnMarket>> playerOnMarketCounter = new HashMap<>();
		Map<String, PlayerOnMarket> transfermarktMap = new HashMap<>();
		if (obj instanceof JSONArray jsonA) {
			jsonA.forEach(item -> {
				if (item instanceof JSONObject playerJson) {

					// Falls deine Klasse PlayerOnMarket einen Konstruktor mit JSONObject hat:
					PlayerOnMarket player = PlayerOnMarket.fromJSON(playerJson);

					transfermarktMap.put(player.getID(), player);
				}
			});
		} else if (obj instanceof JSONObject) {
			JSONObject json = (JSONObject) obj;

			JSONObject transfermarktObj = json.optJSONObject("transfermarktMap");
			if (transfermarktObj != null) {
				for (String key : transfermarktObj.keySet()) {
					PlayerOnMarket player = PlayerOnMarket.fromJSON(transfermarktObj.optJSONObject(key));
					if (player != null) {
						transfermarktMap.put(key, player);
					}
				}
			}

			// playerOnMarketCounter

			JSONObject counterObj = json.optJSONObject("playerOnMarketCounter");
			if (counterObj != null) {
				for (String key : counterObj.keySet()) {
					JSONArray arr = counterObj.optJSONArray(key);
					Set<PlayerWasOnMarket> set = new HashSet<>();
					if (arr != null) {
						for (int i = 0; i < arr.length(); i++) {
							PlayerWasOnMarket player = PlayerWasOnMarket.fromJSON(arr.optJSONObject(i));
							if (player != null) {
								set.add(player);
							}
						}
					}
					playerOnMarketCounter.put(key, set);
				}
			}

			// lastUpdate
			String lastUpdateStr = json.optString("lastUpdate", null);
			if (lastUpdateStr != null) {
				try {
					result.setLastUpdate(Instant.parse(lastUpdateStr));
				} catch (Exception e) {
					// ungültiges Format → null beibehalten
				}
			}

		}
		result.setPlayerOnMarketCounter(playerOnMarketCounter);
		result.setTransfermarktMap(transfermarktMap);

		return result;
	}

	/**
	 * Fügt einen Spieler zum Transfermarkt hinzu und protokolliert den
	 * Transfermarkt-Aufenthalt.
	 * <p>
	 * Ein neuer Eintrag im Set {@code playerOnMarketCounter} wird nur erstellt,
	 * wenn der Spieler zu einem neuen Zeitpunkt ({@code date}) auf den Markt
	 * gesetzt wurde. Ein bereits existierender Eintrag mit gleichem {@code date}
	 * wird nicht dupliziert.
	 *
	 * @param playerID         Eindeutige ID des Spielers
	 * @param SpielerName      Name des Spielers
	 * @param date             Zeitpunkt (Datum/Zeit), an dem der Spieler auf den
	 *                         Markt gesetzt wurde
	 * @param preis            Aktueller Marktpreis des Spielers
	 * @param remainingDate    Datum, bis zu dem der Spieler auf dem Markt bleibt
	 * @param remainingSeconds Verbleibende Sekunden auf dem Markt
	 * @param setOnMarket      Status des Setzens auf den Markt
	 * @param verein           Verein, dem der Spieler aktuell angehört
	 * @param punkte           Aktuelle Punktzahl des Spielers
	 * @param position         Position des Spielers (z.B. Stürmer, Mittelfeld)
	 * @param wert             Aktueller Marktwert des Spielers
	 * @param status           Aktueller Status (z.B. verletzt, gesperrt)
	 */

	public void addOnMarketPlayer(String playerID, String playerName, String date, int preis, String remainingDate, long remainingSeconds, String setOnMarket, String verein, int punkte, String position, int wert, String status, String ownerId) {
		if (this.transfermarktMap == null) {
			this.transfermarktMap = new HashMap<>();
		}
		if (this.playerOnMarketCounter == null) {
			this.playerOnMarketCounter = new HashMap<>();
		}
		PlayerOnMarket player = new PlayerOnMarket(playerID, playerName, date, preis, remainingDate, remainingSeconds, setOnMarket, verein, punkte, position, wert, status, ownerId);
		this.transfermarktMap.put(playerID, player);

		Set<PlayerWasOnMarket> playerSet = getSetForPlayer(playerID);
		for (String entry : this.playerOnMarketCounter.keySet()) {
			if (entry.equals(playerID)) {
				// Nur hinzufügen, wenn kein Eintrag mit gleichem date bereits existiert

				boolean exists = playerSet.stream().anyMatch(p -> p.getDate().equals(date));
				if (!exists && (player.getOwnerId().equals("0") || player.getOwnerId().equals("1"))) {
					playerSet.add(new PlayerWasOnMarket(playerID, playerName, date, preis, wert));
				}
			}
		}
		this.playerOnMarketCounter.put(playerID, playerSet);
	}

	private Set<PlayerWasOnMarket> getSetForPlayer(String playerID) {
		if (this.playerOnMarketCounter == null) {
			this.playerOnMarketCounter = new HashMap<>();
		}
		if (this.playerOnMarketCounter.containsKey(playerID)) {
			return this.playerOnMarketCounter.get(playerID);
		} else {
			return new HashSet<PlayerWasOnMarket>();
		}

	}

}
