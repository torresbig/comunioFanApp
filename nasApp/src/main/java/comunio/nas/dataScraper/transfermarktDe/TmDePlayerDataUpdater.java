package comunio.nas.dataScraper.transfermarktDe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.TransfermarktVerletzter;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.player.Spielerdaten;
import comunio.nas.objects.player.Status;
import comunio.nas.util.ClubMapper;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.StatusManager;
import comunio.nas.util.player.PlayerHelper;
import comunio.nas.util.player.PlayerMatcher;

public class TmDePlayerDataUpdater {

	private static final Logger LOGGER = LogManager.getLogger(TmDePlayerDataUpdater.class);

	/**
	 * Liest Transfermarkt-Daten für einen Spieler aus der lokalen Spielerdatenbank
	 * aus und aktualisiert sie.
	 * 
	 * Es werden folgende Szenarien behandelt: - Spieler, die nicht mehr in der Liga
	 * sind, werden übersprungen. - Spieler mit bereits vorhandenem
	 * Transfermarkt-Link werden nicht erneut abgefragt. - Fehlschläge (Datum
	 * 01.01.1900) können bei `faildRetry == true` erneut versucht werden.
	 * 
	 * Die Methode nutzt den Spielernamen, Verein (aus der lokalen DB) und
	 * alternative Namen (`possibleNames`) für eine erweiterte Suche.
	 *
	 * @param player     JSONObject eines Spielers aus der lokalen Datenbank
	 * @param clubDB     JSONArray mit Vereinsdaten für Verein-Name-Mapping
	 * @param faildRetry true, um Spieler mit vorherigem Fehlschlag erneut
	 *                   abzufragen
	 * @param log        StringBuilder für ausführliches Logging der Suche
	 * @return Ein Statusstring: "OK", "NOT_FOUND", "NO_DATA", "HAS_ALREADY_DATA",
	 *         "FAIL"
	 */

	public static String getTmDeDataForPlayer(JSONObject player, JSONArray clubDB, boolean faildRetry, StringBuilder log, LastUpdates lastUpdates, 	StatusManager statusManager) {
		JSONObject data = player.optJSONObject("data");
		if (data == null) {
			return "NO_DATA";
		}

		String playerId = player.optString("id", "0");
		Status playerStatus = StatusManager.getStatusForPlayer(playerId, statusManager.getInjuryDB()) ;
		
		if (playerStatus.getStatus() == SpielerStatus.NICHT_IN_LIGA) {
			return "HAS_ALREADY_DATA";
		}

		JSONObject tmDe = data.optJSONObject("transfermarktDoDe");
		if (tmDe == null) {
			tmDe = new JSONObject();
			data.put("transfermarktDoDe", tmDe);
		}

		// Prüfen auf Fehlschlag-Datum
		if ("01.01.1900".equals(tmDe.optString("date")) && !faildRetry) {
			return "FAIL";
		}

		// Bereits vorhanden? Dann nichts tun
		if (tmDe.has("link") && !tmDe.optString("link").isBlank()) {
			return "HAS_ALREADY_DATA";
		}

		// Verein in Daten prüfen
		if (!data.has("verein") || data.isNull("verein") || data.optString("verein").isBlank()) {
			return "HAS_ALREADY_DATA";
		}

		String clubName = ClubMapper.getClubnameFromComunioId(data.getString("verein"), clubDB);
		if (clubName.isBlank()) {
			return "NO_DATA";
		}

		try {
			String spielerName = player.optString("name", "");
			String txt = "Suche Spieler '" + spielerName + "' bei Transfermarkt für Verein '" + clubName + "'...";
			System.out.println(txt);
			log.append(txt).append(System.lineSeparator());

			// mögliche alternative Namen extrahieren
			List<String> possibleNames = new ArrayList<>();
			JSONArray posNameArray = data.optJSONArray("possibleNames");
			if (posNameArray != null) {
				for (int i = 0; i < posNameArray.length(); i++) {
					String altName = posNameArray.optString(i);
					if (altName != null && !altName.isBlank()) {
						possibleNames.add(altName);
					}
				}
			}

			// Suche bei Transfermarkt
			JSONObject tmDePlayerData = TmDePlayerFinder.searchPlayer(spielerName, clubName, possibleNames);
			if (tmDePlayerData == null) {
				txt = "Kein Treffer bei Transfermarkt für Spieler " + spielerName + " / Verein: " + clubName;
				System.out.println(txt);
				log.append(txt).append(System.lineSeparator());

				// Fehlschlag markieren
				tmDe.put("date", "01.01.1900");
				tmDe.put("haveToCheck", true);
				return "NOT_FOUND";
			}

			// Erfolgreiche Datenaktualisierung
			tmDe.put("date", new ComunioDate().toString());
			tmDe.put("name", tmDePlayerData.optString("name"));
			tmDe.put("link", tmDePlayerData.optString("link"));
			tmDe.put("id", tmDePlayerData.optString("id"));
			tmDe.put("position", tmDePlayerData.optString("position"));
			tmDe.put("nationalitaet", tmDePlayerData.optString("nationalitaet"));
			tmDe.put("alter", tmDePlayerData.optInt("alter"));
			tmDe.put("haveToCheck", tmDePlayerData.optBoolean("haveToCheck", false));

			long marktwert = tmDePlayerData.optLong("marktwert");
			if (marktwert > 0) {
				tmDe.put("marktwert", marktwert);
				data.put("realWert", marktwert);
			}

			data.put("transfermarktDoDe", tmDe);

			// Zusätzliche Namensvarianten speichern
			JSONArray posNames = data.optJSONArray("possibleNames");
			if (posNames == null)
				posNames = new JSONArray();

			String name = tmDePlayerData.optString("name");
			if (!containsValue(posNames, name)) {
				posNames.put(name);
			}

			String[] split = name.split(" ");
			if (split.length > 1) {
				String nachname = split[split.length - 1];
				if (!containsValue(posNames, nachname)) {
					posNames.put(nachname);
				}
				String initial = name.substring(0, 1);
				String kurzform = initial + ". " + nachname;
				if (!containsValue(posNames, kurzform)) {
					posNames.put(kurzform);
				}
			}

			data.put("possibleNames", posNames);

			// Bei vorhandenem Link Spielerdaten laden
			if (!tmDe.optString("link").isBlank()) {
				getTransfermarktDeSpielerDaten(data, lastUpdates);
			}

			log.append("Spieler erfolgreich aktualisiert: ").append(name).append(" (").append(clubName).append(")").append(System.lineSeparator());
			return "OK";

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warning("Thread unterbrochen: {} " + e.getMessage() + "/n" + log);
			return "FAIL";
		} catch (Exception e) {
			LOGGER.warning("Fehler beim Abruf von Transfermarkt-Daten für {}: {} " + player.optString("name") + " /n exception " + e.getMessage() + "/n" + log);
			return "FAIL";
		}
	}

	/**
	 * Aktualisiert alle Spieler in der lokalen Datenbank, bei denen noch keine
	 * Transfermarkt-Daten vorliegen oder ein erneuter Versuch aufgrund von Fehlern
	 * gewünscht ist.
	 *
	 * Es werden maximal 30 Spieler abgefragt (API-freundliche Begrenzung). Zwischen
	 * den Anfragen wird eine Pause von ca. 1,5 Sekunden eingelegt.
	 *
	 * @param playerDBObject Das Objekt mit dem gesamten Spieler-Datenbank-JSONArray
	 *                       unter key "playerDB"
	 * @param clubDB         JSONArray mit Vereinsinformationen (zur
	 *                       Clubnamenauflösung)
	 * @param faildRetry     true, um auch Spieler mit vorherigen Fehlschlägen neu
	 *                       abzufragen
	 */
	public static void updateAllPlayerWithMissedData(JSONObject playerDBObject, JSONArray clubDB, boolean faildRetry, LastUpdates lastUpdates, StatusManager statusManager) {
		StringBuilder log = new StringBuilder();

		int playerCountUpdate = 0;
		int playerCountNotFound = 0;
		int playerAlreadyData = 0;
		int playerNoData = 0;
		int playerFail = 0;
		int gesamtCount = 0;
		final int MAX_REQUEST = faildRetry ? 25 : 50;

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.warning("KEINE SPIELERDATENBANK vorhanden!");
			return;
		}

		for (int i = 0; i < playerDB.length(); i++) {
			if (gesamtCount >= MAX_REQUEST) {
				log.append("Maximale Anfragen für Transfermarkt.de erreicht: ").append(MAX_REQUEST).append(System.lineSeparator());
				break;
			}

			JSONObject player = playerDB.getJSONObject(i);

			String status = getTmDeDataForPlayer(player, clubDB, faildRetry, log, lastUpdates, statusManager);

			switch (status) {
			case "OK" -> {
				playerCountUpdate++;
				gesamtCount++;
			}
			case "NOT_FOUND" -> {
				playerCountNotFound++;
				gesamtCount++;
			}
			case "HAS_ALREADY_DATA" -> playerAlreadyData++;
			case "NO_DATA" -> playerNoData++;
			case "FAIL" -> playerFail++;
			}

			// API-Freundlichkeit: Pause nach jedem gültigen Anfrageversuch
			if (status.equals("OK") || status.equals("NOT_FOUND")) {
				String name = player.getString("name");
				String id = player.getString("id");

				StringBuilder zwischenInfo = new StringBuilder();
				zwischenInfo.append("#").append(i).append(" Spieler: ").append(name).append(" (ID: ").append(id).append(")").append(" wurde Abgefragt: updateAllPlayerWithMissedData");
			if (faildRetry) {
					LOGGER.info("FaildRetry TRUE! : " + zwischenInfo.toString());
				} else {
					LOGGER.info(zwischenInfo.toString());
				}
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					LOGGER.warning("Thread interrupted during sleep: {} " + e.getMessage());
					break;
				}
			}
		}
		if (faildRetry) {
			log.append("Update (2. Durchgang) abgeschlossen – Spieler mit fehlenden Daten:").append(System.lineSeparator());
		} else {
			log.append("Update abgeschlossen – Spieler mit fehlenden Daten:").append(System.lineSeparator());
		}

		log.append("  Erfolgreich aktualisiert: ").append(playerCountUpdate).append(System.lineSeparator());
		log.append("  Kein Update / Daten vorhanden: ").append(playerAlreadyData).append(System.lineSeparator());
		log.append("  Kein Update / Nicht gefunden: ").append(playerCountNotFound).append(System.lineSeparator());
		log.append("  Kein Update / Player hat kein Data-Object: ").append(playerNoData).append(System.lineSeparator());
		log.append("  Kein Update / Fehler (FAIL): ").append(playerFail).append(System.lineSeparator());

		LOGGER.info(log.toString());

		// Erneuter Versuch, wenn keine Updates/Nicht gefunden aber Fehler da sind
		if (playerCountNotFound == 0 && playerCountUpdate == 0 && playerFail > 0 && !faildRetry) {
			LOGGER.info("2. Update-Durchgang wird gestartet, da vorher Fehler bestanden und sonst alle Spieler aktualisiert erscheinen.");

			updateAllPlayerWithMissedData(playerDBObject, clubDB, true, lastUpdates, statusManager);
		}
	}

	/**
	 * Prüft, ob der String-Wert in einem JSONArray von Strings enthalten ist
	 * (case-insensitive).
	 *
	 * @param array JSONArray mit Strings
	 * @param value Zu suchender String
	 * @return true wenn vorhanden, sonst false
	 */
	public static boolean containsValue(JSONArray array, String value) {
		if (array == null || value == null)
			return false;
		for (int i = 0; i < array.length(); i++) {
			if (value.equalsIgnoreCase(array.optString(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Aktualisiert alle Spieler mit gültigem Transfermarkt-Link. Im zweiten
	 * Durchlauf werden die Spieler zuerst mit dem ältesten Update-Datum behandelt.
	 * 
	 * @param playerDBObject Die Datenbank als JSONObject.
	 * @param updateAll      true, wenn alle Spieler aktualisiert werden sollen.
	 * @param secondTry      true, wenn der zweite Versuch ausgeführt wird
	 *                       (Sortierung nach Datum).
	 */
	public static void updateAllPlayerWithLink(JSONObject playerDBObject, boolean updateAll, boolean secondTry, LastUpdates lastUpdates, StatusManager statusManager) {
		int playerCountUpdate = 0;
		int playerCountFail = 0;
		int playerCountSkip = 0;
		LOGGER.info("Beginn Spieler-Update mit bestehenden Transfermarkt-Links. UpdateAll-Modus: " + updateAll);

		final int MAX_REQUEST = secondTry ? 10 : 25;

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.log(Level.WARNING, "KEINE SPIELERDATENBANK vorhanden!");
			return;
		}

		ArrayList<JSONObject> allPlayerList = new ArrayList<>();
		for (int i = 0; i < playerDB.length(); i++) {
			allPlayerList.add(playerDB.getJSONObject(i));
		}

		// Spieler nach 'data.transfermarktDoDe.date' aufsteigend sortieren
		allPlayerList.sort(Comparator.comparing(obj -> {
			JSONObject data = obj.optJSONObject("data");
			if (data == null)
				return new ComunioDate("01.01.1970").getTime();
			JSONObject tmDe = data.optJSONObject("transfermarktDoDe");
			if (tmDe == null)
				return new ComunioDate("01.01.1970").getTime();
			String dateStr = tmDe.optString("date", "01.01.1970");
			return new ComunioDate(dateStr).getTime();
		}));

		ArrayList<JSONObject> playerListSkipped = new ArrayList<>();

		for (int i = 0; i < allPlayerList.size(); i++) {
			if (playerCountUpdate >= MAX_REQUEST) {
				LOGGER.warning("Maximale Anfragen für Transfermarkt.de erreicht: " + MAX_REQUEST);
				break;
			}
			JSONObject player = allPlayerList.get(i);
			JSONObject data = player.optJSONObject("data");

			if (data == null) {
				playerCountFail++;
				continue;
			}
			String playerId = player.optString("id", "0");
			Status playerStatus = StatusManager.getStatusForPlayer(playerId, statusManager.getInjuryDB());

			JSONObject tmDe = data.optJSONObject("transfermarktDoDe");

			if (tmDe == null) {
				playerCountFail++;
				continue;
			}

			String link = tmDe.has("link") ? tmDe.getString("link") : "";

			if (link == null || link.isBlank()) {
				playerCountFail++;
				continue;
			}

			// Spieler überspringen, wenn er nicht mehr in der Liga ist.
			if (playerStatus.getStatus() == SpielerStatus.NICHT_IN_LIGA) {
				continue;
			}
			Spielerdaten spielerDaten = Spielerdaten.fromJSON((JSONObject) data.opt("spielerDaten"));

			if (updateAll || spielerDaten.getLastUpdate() == null || spielerDaten.getLastUpdate().before(new ComunioDate().addDays(-45))) {

				try {
					long sleeptime = 10000 + (long) (Math.random() * 5000);
					System.out.println("wartezeit: " + sleeptime);
					Thread.sleep(sleeptime);

					if (getTransfermarktDeSpielerDaten(data, lastUpdates)) {
						playerCountUpdate++;
						LOGGER.log(Level.INFO, "#" + playerCountUpdate + " " + "Spieler: " + player.getString("name") + " erfolgreich aktualisiert! ");
					} else {
						playerCountFail++;
						LOGGER.log(Level.WARNING, "Spieler: " + player.getString("name") + " NICHT aktualisiert! ");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				playerCountSkip++;
				playerListSkipped.add(player);
			}
		}

		// Im zweiten Versuch erneut übersprungene Spieler updaten
		if (playerCountUpdate == 0 && playerCountSkip > 0 && playerListSkipped.size() > 0 && !secondTry) {
			updateAllPlayerWithLink(playerDBObject, true, true, lastUpdates, statusManager);
		}

		LOGGER.log(Level.INFO, "Transfermarkt.de Updater ausgeführt. Erfolgreich aktualisiert: " + playerCountUpdate + " - fehlerhaft: " + playerCountFail + " - übersprungen: " + playerCountSkip);
	}

	/**
	 * Parst detaillierte Spielerinformationen direkt von der Spieler-Seite auf
	 * Transfermarkt.de.
	 * <p>
	 * Diese Methode lädt das HTML-Dokument eines Spielers mittels Jsoup, extrahiert
	 * Basisinformationen wie Name, Geburtstag, Größe, Position, Fuß, Nationalität
	 * sowie Leistungsdaten (z. B. Spiele, Tore, Marktwert) und trägt diese in die
	 * übergebene Spielerdatenstruktur ein.
	 * <p>
	 * Die Informationen werden sowohl im Objekt {@code spielerDaten} als auch im
	 * Objekt {@code transfermarktDoDe} abgelegt.
	 *
	 * @param playerData Das Spieler-JSONObject mit vorhandenen Transfermarkt-Link
	 *                   unter "transfermarktDoDe.link".
	 * @return true, wenn das Update erfolgreich war; false bei Fehler oder
	 *         fehlenden Daten.
	 */
	public static boolean getTransfermarktDeSpielerDaten(JSONObject playerData, LastUpdates lastUpdates) {
		LOGGER.log(Level.INFO, "Spielerdaten von Transfermarkt.de laden...");

		ComunioDate lastUpdate = PlayerHelper.getDateByPath(playerData, "transfermarktDoDe.spielerDaten.lastUpdate");

		if (lastUpdate != null) {
			lastUpdate.addDays(30);
			if (lastUpdate.after(new ComunioDate()) || lastUpdate.equals(new ComunioDate())) {
				// LOGGER.log(Level.INFO, "Spielerdaten von Transfermarkt.de nicht neu geladen.
				// Sind noch aktuell! LastUdate: " + lastUpdate.toString());
				return false;
			}
		}
		try {
			JSONObject tmDe = playerData.optJSONObject("transfermarktDoDe");
			Spielerdaten spielerDaten = Spielerdaten.fromJSON((JSONObject) playerData.optJSONObject("spielerDaten", new JSONObject()));
			if (tmDe == null) {
				// LOGGER.log(Level.WARNING, "kein TransfermarktDoDe daten im Spielerobject!
				// playerData-Object: " + playerData.toString());
				return false;
			}
			String link = tmDe.has("link") ? tmDe.getString("link") : "";

			// Lade das HTML-Dokument der Spieler-Seite
			// Baue die Verbindung mit allen wichtigen Headern auf
			if (link == null || link.isBlank()) {
				// LOGGER.log(Level.WARNING, "kein Spielerlink! tmDe-Object: " +
				// tmDe.toString());
				return false;
			}
			Document doc = null;
			try {
				doc = Jsoup.connect(link)//
						.headers(HttpHeaderUtil.getRandomHeaders())//
						.ignoreContentType(true)//
						.timeout(25000)//
						.get();

			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Fehler beim laden der Daten von Transfermarkt.de", e);
				long sleeptime = 15000 + (long) (Math.random() * 5000);
				System.out.println("wartezeit: " + sleeptime);
				Thread.sleep(sleeptime);

				try {
					doc = Jsoup.connect(link)//
							.headers(HttpHeaderUtil.getRandomHeaders())//
							.ignoreContentType(true)//
							.timeout(25000)//
							.get();
				} catch (Exception e2) {
					LOGGER.log(Level.WARNING, "2ter Durchgang: Fehler beim laden der Daten von Transfermarkt.de", e2);
					return false;
				}
			}

			// --- Stammdaten extrahieren und ins SpielerDaten-Objekt schreiben ---
			Element header = doc.selectFirst("header.data-header");
			if (header != null) {
				// Name & Trikotnummer
				Element nameElem = header.selectFirst("h1.data-header__headline-wrapper");
				if (nameElem != null) {
					// String name = nameElem.ownText().trim();
					// spielerDaten.setName(name);
					Element numElem = nameElem.selectFirst("span.data-header__shirt-number");
					if (numElem != null) {
						String num = numElem.text().replace("#", "").trim();
						if (!num.isEmpty())
							spielerDaten.setTrikotNummer(Integer.parseInt(num));
					}
				}
				// Geburtstag/Alter
				Element gebElem = header.selectFirst("li:contains(Geb./Alter:) span.data-header__content");
				if (gebElem != null) {
					String geb = gebElem.text().split("\\(")[0].trim();
					spielerDaten.setGeburtstag(geb);
				}
				// Staatsbürgerschaft
				Element natElem = header.selectFirst("li:contains(Staatsbürgerschaft:) span.data-header__content");
				if (natElem != null)
					spielerDaten.setNationalitaet(natElem.text().trim());
				// Größe
				Element groesseElem = header.selectFirst("li:contains(Größe:) span.data-header__content");
				if (groesseElem != null)
					spielerDaten.setGroesse(groesseElem.text().replace("m", "").trim());
				// Position
				Element posElem = header.selectFirst("li:contains(Position:) span.data-header__content");
				if (posElem != null)
					spielerDaten.setHauptposition(posElem.text().trim());
				// Fuß
				Element fussElem = doc.selectFirst("span.info-table__content--regular:contains(Fuß:) + span.info-table__content--bold");
				if (fussElem != null) {
					spielerDaten.setFuss(fussElem.text().trim());
				}

			}

			// Nationalmannschaftsdaten
			Element natSpieleElem = doc.selectFirst("li:contains(Länderspiele/Tore:)");
			if (natSpieleElem != null) {
				Pattern p = Pattern.compile("Länderspiele/Tore:\\s*(\\d+)\\s*/\\s*(\\d+)");
				Matcher m = p.matcher(natSpieleElem.text());
				if (m.find()) {
					spielerDaten.setLaenderspiele(Integer.parseInt(m.group(1)));
					spielerDaten.setLaenderspielTore(Integer.parseInt(m.group(2)));
				}
			}
			spielerDaten.setLastUpdate(new ComunioDate());
			// TODO: DATUM RAUS
			lastUpdates.setTransfermarktDe(Instant.now());
			playerData.put("spielerDaten", spielerDaten.toJSON());

			// --- Leistungsdaten extrahieren und in die Map legen ---

			// Marktwert (Integer, Mio/Tsd)
			Element mwElem = doc.selectFirst("div.data-header__box--small a.data-header__market-value-wrapper");
			if (mwElem == null)
				mwElem = doc.selectFirst("div.current-value a");
			if (mwElem != null) {
				String mwText = mwElem.text().replace("\u00a0", " ").replace("€", "").trim();
				if (mwText.contains("Letzte ")) {
					mwText = mwText.split(" Letzte")[0].trim();
				}
				playerData.put("realWert", TmDePlayerFinder.parseMarktwert(mwText));
			}

			// Startelf-Quote, Spielminuten-Quote, Torbeteiligungen-Quote (Prozent)
			Elements gaugeDivs = doc.select("div.tm-player-performance__gauge");
			JSONObject leistungsDaten = new JSONObject();
			for (Element gauge : gaugeDivs) {
				String label = gauge.selectFirst("span.percentage-term").text().trim();
				String value = gauge.selectFirst("span.percentage-value").text().trim();
				if (label.contains("Startelf")) {
					leistungsDaten.put("Startelf-Einsätze (Prozent)", Integer.parseInt(value));
				} else if (label.contains("Spielminuten")) {
					leistungsDaten.put("Spielminuten (Prozent)", Integer.parseInt(value));
				} else if (label.contains("Torbeteiligungen")) {
					leistungsDaten.put("Torbeteiligungen (Prozent)", Integer.parseInt(value));
				}
			}

			// Spiele, Tore, Vorlagen, Karten
			Elements perfStats = doc.select("div.tm-player-performance__statistic-list li.tm-player-performance__stats-list-item");

			for (Element stat : perfStats) {
				String label = stat.selectFirst("span[data-testid=performance-slide-stat-item-term]").text().trim();
				String value = stat.selectFirst("a.tm-player-performance__stats-list-item-value").text().trim();
				try {
					if (!value.isBlank())
						leistungsDaten.put(label, Integer.parseInt(value));
				} catch (NumberFormatException e) {
					LOGGER.warning("Fehler beim Parsen der Leistungskennzahl '" + label + "': " + value);
				}

			}
			// SpielerDaten-Objekt in die Map legen
			tmDe.put("leistungsDaten", leistungsDaten);
			tmDe.put("date", new ComunioDate().toString());
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fehler beim laden der Daten von Transfermarkt.de für spielerObject:  " + playerData.toString(), e);
			return false;
		}
	}

	/**
	 * Führt ein Update des Verletzungsstatus aller Spieler der Datenbank anhand der
	 * öffentlich zugänglichen Verletztenliste von Transfermarkt.de durch.
	 * <p>
	 * Dabei werden verschiedene Matching-Strategien verwendet: exakte ID, exakter
	 * Name, fuzzy Matching mit Levenshtein. Zusätzlich werden Transfermarkt-Daten
	 * wie Nationalität, Link und Marktwert in die Daten aufgenommen. Spieler ohne
	 * Verein erhalten den Status "NICHT_IN_LIGA". Statusänderungen werden, außer
	 * für "AKTIV", historisiert.
	 * <p>
	 * Für Statusänderungen werden außerdem News-Objekte erzeugt, sofern diese noch
	 * nicht existieren.
	 *
	 * @param playerDB     Die gesamte lokale Spieler-Datenbank als
	 *                     {@code JSONArray}.
	 * @param clubsMapping Ein Mapping-JArray von DB-Vereins-IDs auf
	 *                     Transfermarkt-Vereinsnamen.
	 * @param newsManager  Das News-Management-Objekt, das neue Statusänderungs-News
	 *                     prüft und speichert.
	 * @param LOGGER       Der Logger zur Protokollierung während des Updates.
	 */
	public static void updateVerletzteVonTransfermarkt(JSONObject playerDBObject, JSONArray clubsMapping, NewsManager newsManager, Logger LOGGER, LastUpdates lastUpdates, StatusManager statusManager) {
		try {
			Map<String, String> dbIdToTmName = ClubMapper.getClubMapDbIdToTmName(clubsMapping);

			JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
			if (playerDB == null) {
				LOGGER.log(Level.WARNING, "KEINE SPIELERDATENBANK vorhanden!");
				return;
			}

			LOGGER.info("Starte Verletzten-Update von transfermarkt.de ...");
			String url = "https://www.transfermarkt.de/bundesliga/verletztespieler/wettbewerb/L1/plus/1";
			Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();

			// Verletztenliste parsen
			List<TransfermarktVerletzter> verletzte = new ArrayList<>();
			Elements rows = doc.select("tr.odd, tr.even");
			for (Element row : rows) {
				Element innerTable = row.selectFirst("table.inline-table");
				if (innerTable == null)
					continue;
				Element nameLink = innerTable.selectFirst("td.hauptlink > a[title][href]");
				if (nameLink == null)
					continue;
				String name = nameLink.text().trim();
				String href = nameLink.attr("href");
				String id = "";
				if (href != null && href.contains("/spieler/")) {
					String[] parts = href.split("/");
					for (int i = 0; i < parts.length; i++) {
						if (parts[i].equals("spieler") && i + 1 < parts.length) {
							id = parts[i + 1];
							break;
						}
					}
				}
				String link = "https://www.transfermarkt.de" + href;
				Element injuryTd = row.selectFirst("td.links");
				String verletzung = injuryTd != null ? injuryTd.text().trim() : "";
				Element seitTd = injuryTd != null ? injuryTd.nextElementSibling() : null;
				String seit = seitTd != null ? seitTd.text().trim() : "";
				Element bisTd = seitTd != null ? seitTd.nextElementSibling() : null;
				String bis = bisTd != null ? bisTd.text().trim() : "";
				Elements zentriertTds = row.select("td.zentriert");
				String nation = "";
				if (zentriertTds.size() >= 3) {
					Element nationTd = zentriertTds.get(2);
					Element nationImg = nationTd.selectFirst("img.flaggenrahmen");
					if (nationImg != null) {
						nation = nationImg.attr("title").trim();
					}
				}
				Element marketValueTd = row.selectFirst("td.rechts");
				String marktwert = marketValueTd != null ? marketValueTd.text().trim() : "";
				Element vereinTd = row.selectFirst("td.zentriert.no-border-rechts a[title][href]");
				String tmVereinName = "";
				if (vereinTd != null) {
					tmVereinName = vereinTd.attr("title").trim();
				}
				Set<String> nameVariants = PlayerMatcher.generatePossibleNames(name);
				verletzte.add(new TransfermarktVerletzter(name, id, verletzung, seit, bis, link, nation, marktwert, tmVereinName, nameVariants));
			}
			LOGGER.info("Gefundene verletzte Spieler: " + verletzte.size());

			int updated = 0, reaktiviert = 0, nichtInLiga = 0;
			String abfrageDatum = new ComunioDate().toString();

			for (int i = 0; i < playerDB.length(); i++) {
				JSONObject spieler = playerDB.getJSONObject(i);
				JSONObject data = spieler.getJSONObject("data");
				String playerId = spieler.optString("id", "0");
				String playerName = spieler.optString("name", "N/A");
				Status playerStatus = StatusManager.getStatusForPlayer(playerId, statusManager.getInjuryDB());
				JSONObject spielerDaten = data.optJSONObject("spielerDaten");
				if (spielerDaten == null) {
					spielerDaten = new JSONObject();
					data.put("spielerDaten", spielerDaten);
				}
				JSONObject tm = data.optJSONObject("transfermarktDoDe");
				if (tm == null) {
					tm = new JSONObject();
					data.put("transfermarktDoDe", tm);
				}
				String tmId = (tm.has("id")) ? String.valueOf(tm.opt("id")) : "";
				String tmName = (tm.has("name")) ? tm.optString("name", "") : "";
				String spielerName = spieler.optString("name", "");
				Set<String> possibleNames = new HashSet<>();
				possibleNames.add(spielerName.trim().toLowerCase());
				JSONArray altNames = data.optJSONArray("possibleNames");
				if (altNames != null) {
					for (int j = 0; j < altNames.length(); j++) {
						possibleNames.add(altNames.getString(j).trim().toLowerCase());
					}
				}
				String vereinDbId = data.optString("verein", "");
				String vereinDbTmName = dbIdToTmName.getOrDefault(vereinDbId, "").toLowerCase();

				// --- Matching ---
				TransfermarktVerletzter match = null;
				// 1. Exakt nach transfermarktDoDe.id
				if (tmId != null && !tmId.equals("0") && !tmId.isEmpty()) {
					for (TransfermarktVerletzter v : verletzte) {
						if (v.id.equals(tmId)) {
							match = v;
							break;
						}
					}
				}
				// 2. Exakt nach transfermarktDoDe.name
				if (match == null && tmName != null && !tmName.isEmpty()) {
					for (TransfermarktVerletzter v : verletzte) {
						if (v.name.equalsIgnoreCase(tmName)) {
							match = v;
							break;
						}
					}
				}
				// 3. Fuzzy auf Namensvarianten und Vereinsnamen (Mapping)
				if (match == null) {
					boolean matchFound = false;
					TransfermarktVerletzter fuzzyMatch = null;
					for (int j = 0; j < verletzte.size() && !matchFound; j++) {
						TransfermarktVerletzter v = verletzte.get(j);
						boolean vereineMatchen = ClubMapper.vergleichClubNames(v.tmVereinName, vereinDbTmName);
						if (!vereineMatchen)
							continue;
						for (String pn : possibleNames) {
							for (String tmNameVar : v.nameVariants) {
								String pnNorm = pn.trim().toLowerCase();
								String tmNorm = tmNameVar.trim().toLowerCase();
								if (!pnNorm.isEmpty() && !tmNorm.isEmpty() && pnNorm.equals(tmNorm)) {
									match = v;
									matchFound = true;
									break;
								}
								int lev = LevenshteinDistance.getDefaultInstance().apply(pnNorm, tmNorm);
								double similarity = 1.0 - ((double) lev / Math.max(pnNorm.length(), tmNorm.length()));
								if (similarity > 0.8 && fuzzyMatch == null) {
									fuzzyMatch = v;
								} else if (similarity > 0.8 && fuzzyMatch != null) {
									fuzzyMatch = null;
									matchFound = true;
									break;
								}
							}
							if (matchFound)
								break;
						}
					}
					if (match == null && fuzzyMatch != null) {
						match = fuzzyMatch;
					}
				}

				// --- Zusätzliche Felder auslesen und eintragen, falls match ---
				if (match != null) {
					if (!tm.has("link") || tm.optString("link").isEmpty()) {
						tm.put("link", match.link);
					}
					if (!tm.has("name") || tm.optString("name").isEmpty()) {
						tm.put("name", match.name);
					}
					if (!match.nation.isEmpty()) {
						spielerDaten.put("nationalitaet", match.nation);
					}
					if (!match.marktwert.isEmpty()) {
						long realWert = TmDePlayerFinder.parseMarktwert(match.marktwert);
						data.put("realWert", realWert);
					}
				}

				String alterStatus = playerStatus.getStatus().toString();

				// --- Spieler ohne Verein auf NICHT_IN_LIGA setzen ---
				if (vereinDbId == null || vereinDbId.equals("0") || vereinDbId.isEmpty()) {
					if (!"NICHT_IN_LIGA".equals(alterStatus)) {

						Status statusObj = new Status();
						statusObj.setNichtInLiga();
						statusManager.addStatusToBuffer(playerId, statusObj);						
						nichtInLiga++;
						LOGGER.info("Spieler " + playerName + " scheint keinen Verein zu haben, Status auf NICHT_IN_LIGA gesetzt.");
					}
					continue;
				}

				// --- Verletzten-Status-Logik ---
				if (match != null) {
					String neuerStatus = "VERLETZT";
					if (!alterStatus.equals(neuerStatus)) {

						Status statusObj = new Status(new ComunioDate(abfrageDatum), neuerStatus, "", match.verletzung, match.seit, match.bis, "transfermarkt");
						statusManager.addStatusToBuffer(playerId, statusObj);	

						updated++;
						LOGGER.info("Spieler " + playerName + " scheint verletzt zu sein (Match in aktueller Verletztenliste), Status auf VERLETZT gesetzt.");
					} else {

						Status statusObj = new Status(new ComunioDate(abfrageDatum), neuerStatus, "", match.verletzung, match.seit, match.bis, "transfermarkt");
						statusManager.addStatusToBuffer(playerId, statusObj);	

					}
				} else {
					// Spieler NICHT mehr verletzt, war aber vorher verletzt
					if ("VERLETZT".equals(alterStatus)) {
						
						Status statusObj = new Status();
						statusObj.setWiederGesund("transfermarkt");
						statusManager.addStatusToBuffer(playerId, statusObj);	
						
						reaktiviert++;
						LOGGER.info("Spieler " + playerName + " scheint nicht mehr verletzt zu sein (kein Match in aktueller Verletztenliste), Status auf AKTIV gesetzt.");
					}
				}

			}
			LOGGER.info("Transfermarkt: Verletzten-Update abgeschlossen: " + updated + " Spieler auf VERLETZT, " + reaktiviert + " reaktiviert, " + nichtInLiga + " NICHT_IN_LIGA.");
			lastUpdates.setPlayerStatus(new ComunioDate().toInstant());
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Update der Verletzten von transfermarkt.de: " + e.getMessage(), e);
		}
	}
}
