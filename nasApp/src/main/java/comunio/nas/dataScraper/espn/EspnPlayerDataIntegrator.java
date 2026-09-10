package comunio.nas.dataScraper.espn;

import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.player.PlayerHelper;

/**
 * Integriert die ESPN-Profil-Daten (Spielerdaten) in die bestehende
 * Comunio-Spieler-Datenbank.
 * <p>
 * Es werden NUR Felder befüllt, die aktuell leer sind ("" oder "N/A") –
 * vorhandene Werte (z. B. von Transfermarkt) haben Vorrang und werden nicht
 * überschrieben. Die Saison-Statistiken (Spielerstats) werden bewusst NICHT
 * integriert.
 * <p>
 * Die Zuordnung Spieler → Verein erfolgt über das ESPN↔Comunio-Vereins-Mapping
 * und den Spielernamen (normalisiert über {@link PlayerHelper#normalizeName}).
 */
public class EspnPlayerDataIntegrator {

	private static final Logger LOGGER = LogManager.getLogger(EspnPlayerDataIntegrator.class);

	/** Konstruktor privat – statische Nutzung. */
	private EspnPlayerDataIntegrator() {
	}

	/**
	 * Integriert die ESPN-Spielerdaten in die Spieler-DB.
	 *
	 * @param playerDBObject Das Spieler-DB-Objekt (mit "playerDB"-Array)
	 * @param espnPlayers    Die aufbereiteten ESPN-Spieler (aus
	 *                       {@link EspnRosterCollector#collectAllRosters})
	 * @param espnToComunio  Mapping ESPN-Team-ID → Comunio-Vereins-ID
	 * @return Anzahl der aktualisierten Spieler
	 */
	public static int integratePlayerData(JSONObject playerDBObject, JSONObject espnPlayers, Map<String, String> espnToComunio) {
		if (playerDBObject == null || espnPlayers == null) {
			LOGGER.warning("IntegratePlayerData: playerDBObject oder espnPlayers ist null!");
			return 0;
		}

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		JSONArray espnPlayerArray = espnPlayers.optJSONArray("players");
		if (playerDB == null || espnPlayerArray == null) {
			LOGGER.warning("IntegratePlayerData: playerDB oder espnPlayers.players fehlt!");
			return 0;
		}

		int updated = 0;

		// ESPN-Spieler nach Verein gruppieren (für effizientere Suche)
		for (int i = 0; i < espnPlayerArray.length(); i++) {
			JSONObject espnPlayer = espnPlayerArray.getJSONObject(i);
			String espnClubId = espnPlayer.optJSONObject("club").optString("espnId", "");
			String comunioClubId = espnToComunio != null ? espnToComunio.get(espnClubId) : null;
			if (comunioClubId == null || comunioClubId.isEmpty()) {
				continue; // Verein nicht gemappt → überspringen
			}

			String espnName = espnPlayer.optString("name", "");
			String normalizedEspnName = PlayerHelper.normalizeName(espnName);

			// Spieler in der DB suchen (gleicher Verein + Namensübereinstimmung)
			for (int j = 0; j < playerDB.length(); j++) {
				JSONObject dbPlayer = playerDB.optJSONObject(j);
				if (dbPlayer == null) {
					continue;
				}
				JSONObject data = dbPlayer.optJSONObject("data");
				if (data == null) {
					continue;
				}
				String dbClubId = data.optString("verein", "");
				if (!comunioClubId.equals(dbClubId)) {
					continue; // anderer Verein
				}

				String dbName = dbPlayer.optString("name", "");
				if (!PlayerHelper.namesMatchWithInitial(normalizedEspnName, PlayerHelper.normalizeName(dbName))) {
					continue; // Name passt nicht
				}

				// Spieler gefunden → Profil-Daten befüllen (nur wenn leer)
				if (updateSpielerDaten(data, espnPlayer)) {
					updated++;
				}
				break; // nächster ESPN-Spieler
			}
		}

		LOGGER.info("ESPN-Integration: " + updated + " Spieler aktualisiert.");
		return updated;
	}

	/**
	 * Befüllt die {@code data.spielerDaten}-Felder aus den ESPN-Daten.
	 * Überschreibt NUR leere Felder.
	 *
	 * @return true, wenn mindestens ein Feld aktualisiert wurde
	 */
	private static boolean updateSpielerDaten(JSONObject data, JSONObject espnPlayer) {
		JSONObject spielerDaten = data.optJSONObject("spielerDaten");
		if (spielerDaten == null) {
			spielerDaten = new JSONObject();
			data.put("spielerDaten", spielerDaten);
		}

		boolean changed = false;

		// Geburtstag (dd.MM.yyyy)
		changed |= fillIfEmpty(spielerDaten, "geburtstag", espnPlayer.optString("dateOfBirth", ""));

		// Nationalität
		changed |= fillIfEmpty(spielerDaten, "nationalitaet", espnPlayer.optString("nationality", ""));

		// Größe (ESPN: "6' 2\"" → "1,88 m" wäre ideal, aber wir speichern erstmal den Rohwert)
		changed |= fillIfEmpty(spielerDaten, "groesse", espnPlayer.optString("height", ""));

		// Hauptposition
		changed |= fillIfEmpty(spielerDaten, "hauptposition", espnPlayer.optString("position", ""));

		// Trikotnummer (nur setzen, wenn aktuell 0 oder nicht vorhanden)
		String jersey = espnPlayer.optString("jersey", "");
		if (!jersey.isEmpty()) {
			try {
				int jerseyNum = Integer.parseInt(jersey);
				boolean hasTrikot = spielerDaten.has("trikotNummer") && spielerDaten.optInt("trikotNummer", 0) != 0;
				if (!hasTrikot) {
					spielerDaten.put("trikotNummer", jerseyNum);
					changed = true;
				}
			} catch (NumberFormatException e) {
				// Trikotnummer ist kein Integer → ignorieren
			}
		}

		if (changed) {
			spielerDaten.put("lastUpdate", new comunio.nas.objects.orga.ComunioDate().toString());
		}

		return changed;
	}

	/**
	 * Setzt einen Wert nur, wenn das Feld aktuell leer ist ("" oder "N/A").
	 */
	private static boolean fillIfEmpty(JSONObject obj, String key, String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		String current = obj.optString(key, "");
		if (current == null || current.isEmpty() || current.equals("N/A")) {
			obj.put(key, value);
			return true;
		}
		return false;
	}
}