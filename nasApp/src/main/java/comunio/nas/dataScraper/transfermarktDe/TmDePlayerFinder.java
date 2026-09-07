package comunio.nas.dataScraper.transfermarktDe;

import java.util.List;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataVariable.Urls;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.ClubMapper;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.player.PlayerHelper;

public class TmDePlayerFinder {

	private static final Logger LOGGER = LogManager.getLogger(TmDePlayerFinder.class);

	/**
	 * Führt eine Schnellsuche auf transfermarkt.de nach einem Spielernamen und
	 * Verein durch. Dabei wird der Spielernamen flexibel mit Initialen oder
	 * Nachnamen abgeglichen, z.B. wird "Thomas Sborn" als passend zu "T. Sborn"
	 * erkannt.
	 * 
	 * Zusätzlich wird eine optionale Liste von alternativen Namensvarianten (z.B.
	 * Spitznamen) durchsucht, wenn unter dem Hauptnamen kein passender Spieler
	 * gefunden wird.
	 * 
	 * Wenn bei einem Treffer erkannt wird, dass der Vorname im Transfermarkt-Name
	 * komplett entfernt wurde, wird dies im zurückgegebenen JSONObject mit
	 * "vorname_entfernt": true markiert.
	 * 
	 * Es werden maximal drei Seiten der Transfermarkt-Suchergebnisse durchgegangen.
	 * 
	 * @param nameInput     Hauptsuchname des Spielers, z.B. "cerny" oder "Voll"
	 * @param vereinInput   Verein, zu dem der Spieler gehören soll, z.B.
	 *                      "Wolfsburg"
	 * @param possibleNames Liste alternativer Namensvarianten zur ergänzenden Suche
	 *                      (z.B. Spitznamen)
	 * @return JSONObject Spielerinformationen (name, verein, position, alter,
	 *         marktwert, nationalitaet, link) mit evtl. Feld "vorname_entfernt":
	 *         true, oder null wenn kein Match gefunden wurde.
	 * @throws Exception Bei Netzwerk- oder Parsingfehlern
	 */
	public static JSONObject searchPlayer(String nameInput, String vereinInput, List<String> possibleNames) throws Exception {
		StringBuilder log = new StringBuilder();
		int maxPages = 5; // Erhöht von 3 auf 5 für bessere Trefferquote
		
		// Versucht zuerst mit dem Hauptnamen
		for (int site = 1; site <= maxPages; site++) {
			LOGGER.info(site + ". Durchgang für Spielersuche gestartet - Transfermarkt-Seite " + site + " wird durchsucht");
			JSONObject result = searchPlayerSinglePass(nameInput, vereinInput, site, log);
			if (result != null) {
				LOGGER.info(log.toString());
				return result;
			}
			if (site == 1 && nameInput.contains(".")) { // Nachnamen-Suche mit Punkt-Name
				String lastName = extractLastName(nameInput);
				if (!lastName.isEmpty()) {
					
					randomWaiting();
					
					result = searchPlayerSinglePass(lastName, vereinInput, site, log);
					if (result != null) {
						LOGGER.info(log.toString());
						return result;
					}
				}
			}
			if (site < maxPages) {
				randomWaiting();
			}
		}
		
		// Wenn unter dem Hauptnamen kein Ergebnis, versuche alternative Namen aus
		// possibleNames
		if (possibleNames != null && !possibleNames.isEmpty()) {
			for (String altName : possibleNames) {
				for (int site = 1; site <= maxPages; site++) {
					JSONObject result = searchPlayerSinglePass(altName, vereinInput, site, log);
					if (result != null) {
						LOGGER.info("Gefunden mit alternativen Namen '" + altName + "':\n" + log.toString());
						return result;
					}
				}
			}
		}
		
		// Zusätzliche Suchstrategien für bessere Trefferquote
		// 1. Nachname-Suche (extrahiere Nachnamen aus dem Hauptnamen)
		String lastName = extractLastName(nameInput);
		if (!lastName.isEmpty() && !lastName.equals(nameInput)) {
			for (int site = 1; site <= maxPages; site++) {
				JSONObject result = searchPlayerSinglePass(lastName, vereinInput, site, log);
				if (result != null) {
					LOGGER.info("Gefunden mit Nachnamen-Suche '" + lastName + "':\n" + log.toString());
					return result;
				}
			}
		}
		
		// 2. Initial-basierte Suche (z.B. "R. Adam" → "r adam")
		String initialBasedName = extractInitialBasedName(nameInput);
		if (!initialBasedName.isEmpty() && !initialBasedName.equals(nameInput)) {
			for (int site = 1; site <= maxPages; site++) {
				JSONObject result = searchPlayerSinglePass(initialBasedName, vereinInput, site, log);
				if (result != null) {
					LOGGER.info("Gefunden mit Initial-basierter Suche '" + initialBasedName + "':\n" + log.toString());
					return result;
				}
			}
		}
		
		// 3. Transliterationsbasierte Suche für verschiedene Schriftzeichen
		String transliteratedName = transliterateName(nameInput);
		if (!transliteratedName.isEmpty() && !transliteratedName.equals(nameInput)) {
			for (int site = 1; site <= maxPages; site++) {
				JSONObject result = searchPlayerSinglePass(transliteratedName, vereinInput, site, log);
				if (result != null) {
					LOGGER.info("Gefunden mit Transliterations-Suche '" + transliteratedName + "':\n" + log.toString());
					return result;
				}
			}
		}
		
		log.append("Kein passender Spieler gefunden für: " + nameInput + " / " + vereinInput + ".").append(System.lineSeparator());
		LOGGER.info(log.toString());
		ComunioDataUpdater.errorDb.addError(new Error(ErrorType.TRANSFERMARKT_DE_FIND_PLAYER, log.toString()));
		return null;
	}

	/**
	 * Intern: Einzeldurchlauf für eine Seite Suchergebnisse bei Transfermarkt.
	 * Markiert Spieler im Ergebnis mit "vorname_entfernt", wenn erkannt wird, dass
	 * der Vorname entfernt wurde (d.h. Eingabe entspricht nur dem Nachnamen).
	 * 
	 * @param nameInput   Suchname (Haupt- oder alternatives Namelement)
	 * @param vereinInput Vereinsname
	 * @param site        Suchergebnisseite (1 bis max)
	 * @param log         StringBuilder zur Sammlung von Logs
	 * @return JSONObject Spielerobjekt oder null falls kein Treffer
	 * @throws Exception bei Netzwerkfehlern
	 */
	private static JSONObject searchPlayerSinglePass(String nameInput, String vereinInput, int site, StringBuilder log) throws Exception {
		String url = Urls.TMDE_PLAYERSEARCH_WITH_SITES(site, nameInput);
		Document doc;
		try {
			doc = Jsoup.connect(url).headers(HttpHeaderUtil.getRandomHeaders()).timeout(30000).get();
		} catch (Exception e) {
			LOGGER.warning("Fehler bei Spielersuche: " + e.getMessage());
			log.append("Fehler bei Seite: ").append(site).append(" mit Name: ").append(nameInput).append(System.lineSeparator());
			return null;
		}
		JSONArray spielerArray = new JSONArray();
		Elements rows = doc.select("div#player-grid table.items tbody tr");

		for (Element row : rows) {
		    // Name-Extraktion (verbessert für responsive Tabellen)
		    Element nameLink = row.selectFirst("td.hauptlink a[href]");
		    if (nameLink == null) continue;
		    
		    String name = nameLink.text(); // "Eliesse Ben Seghir"
		    String profilLink = nameLink.attr("href");
		    
		    // Verein-Extraktion (korrekt für inline-table Struktur)
		    Element clubLink = row.selectFirst("td.hauptlink a[title]:not([href])");
		    String verein;
		    if (clubLink != null) {
		        verein = clubLink.attr("title").trim();
		    } else {
		        // Fallback für andere Tabellenformate
		        Elements clubElements = row.select("td.zentriert a[title]");
		        verein = clubElements.isEmpty() ? "Karriereende" : clubElements.get(0).attr("title").trim();
		    }
		    
		    // Weitere Extraktionen wie bisher...
		    String position = row.select("td.zentriert:nth-of-type(1)").text().trim();
		    String alterRaw = row.select("td.zentriert:nth-of-type(2)").text().trim();
		    
			int alter = -1;
			try {
				alter = Integer.parseInt(alterRaw);
			} catch (NumberFormatException e) {
//	            log.append("Alter ist kein numerischer Wert: '" + alterRaw + "' bei Spieler: " + name).append(System.lineSeparator());
			}
			String nat = row.select("td:nth-of-type(5) img").attr("title").trim();
			String mwText = row.select("td:nth-of-type(6)").text().trim();
			if (mwText.contains("Letzte ")) {
				mwText = mwText.split(" Letzte")[0].trim();
			}
			long marktwert = parseMarktwert(mwText);

			JSONObject player = new JSONObject();
			player.put("name", name);
			player.put("link", "https://www.transfermarkt.de" + profilLink);
			player.put("id", extractIdFromLink(profilLink));
			player.put("position", position.replace(name, "").trim());
			player.put("verein", verein);
			player.put("alter", alter);
			player.put("nationalitaet", nat);
			player.put("marktwert", marktwert);

			spielerArray.put(player);
		}

		int counter = 0;
		for (int i = 0; i < spielerArray.length(); i++) {
			counter++;
			JSONObject p = spielerArray.getJSONObject(i);

			String verein2 = p.getString("verein");
			boolean clubMatch = ClubMapper.vergleichClubNames(vereinInput, verein2) || ClubMapper.enthaeltClubNames(vereinInput, verein2);

			if (!clubMatch)
				continue;

			// In der Methode searchPlayerSinglePass, ersetzen Sie die Name-Prüfung (um Zeile 170):

			String tmName = p.getString("name");
			boolean nameMatch = false;

			// Verbesserte Name-Matching-Logik:
			if (nameInput.equalsIgnoreCase(tmName)) {
			    nameMatch = true;
			} else if (PlayerHelper.containsSubstringMatch(PlayerHelper.normalizeName(nameInput), PlayerHelper.normalizeName(tmName))) {
			    // Teilvergleich: "Ben Seghir" in "Eliesse Ben Seghir"
			    nameMatch = true;
			} else {
			    // Fallback auf Initial-basierte Übereinstimmung
			    nameMatch = PlayerHelper.namesMatchWithInitial(nameInput, tmName) || 
			                PlayerHelper.namesMatchWithInitial(tmName, nameInput);
			}

			boolean vornameEntfernt = false;

			// Erweitert: Prüfe, ob der eingegebene Name nur Nachname ist und der gefundene
			// Name aber länger (Vorname vorhanden)
			String[] splName = p.getString("name").split(" ");
			if (!nameMatch && splName.length > 1) {
				String lastName = splName[splName.length - 1];
				nameMatch = PlayerHelper.namesMatchWithInitial(nameInput, lastName) || PlayerHelper.namesMatchWithInitial(lastName, nameInput);

				if (nameMatch) {
					vornameEntfernt = true;
					log.append("WARNUNG: Vorname wurde entfernt. Eingabe: '" + nameInput + "', Transfermarkt Name: '" + p.getString("name") + "'").append(System.lineSeparator());
				}
			}

			if (nameMatch && clubMatch) {
				if (vornameEntfernt) {
					p.put("haveToCheck", true);
				}
				log.append("Passender Spieler gefunden: " + p.getString("name") + " / " + p.getString("verein") + " nach " + counter + " Einträgen.").append(System.lineSeparator());
				return p;
			}
		}
		return null;
	}

	private static String extractIdFromLink(String profilLink) {
		String[] parts = profilLink.split("/");
		return parts.length > 0 ? parts[parts.length - 1] : "";
	}

	/**
	 * Konvertiert einen Transfermarkt-Marktwert-String (z. B. „1,00 Mio. €“ oder
	 * „100 Tsd. €“) in eine Long-Zahl in Euro. Falls kein Marktwert vorhanden ist
	 * („-“), wird {@code 0} zurückgegeben.
	 *
	 * @param text Der Marktwert-String aus der Transfermarkt-Tabelle
	 * @return Marktwert in Euro als {@code long}
	 */
	public static long parseMarktwert(String text) {
		if (text == null || text.equals("-"))
			return 0;
		text = text.replace(".", "").replace("€", "").trim();
		if (text.endsWith("Mio"))
			return (long) (Double.parseDouble(text.replace("Mio", "").replace(",", ".")) * 1_000_000);
		if (text.endsWith("Tsd"))
			return (long) (Double.parseDouble(text.replace("Tsd", "").replace(",", ".")) * 1_000);
		return 0;
	}
	
	private static void randomWaiting() {
		// API-Freundlichkeit: kurze Pause vor Anfrage
		long waitingTime = 1000 + (long) (Math.random() * 1000);
		LOGGER.info("Waiting-Time: "+ waitingTime);
		try {
			Thread.sleep(waitingTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Extrahiert den Nachnamen aus einem gegebenen Namen
	 * 
	 * @param fullName Vollständiger Name (z.B. "R. Adam", "Thomas Sborn")
	 * @return Extrahierter Nachname (z.B. "Adam", "Sborn")
	 */
	private static String extractLastName(String fullName) {
	    if (fullName == null || fullName.isEmpty()) {
	        return "";
	    }
	    
	    // Entferne Initialen mit Punkt (z.B. "R. Adam" → "Adam")
	    String withoutInitial = fullName.replaceAll("^[A-Z]\\.\\s*", "");
	    
	    // Wenn der Name nur einen Teil hat, gib ihn zurück
	    if (withoutInitial.isEmpty()) {
	        return fullName;
	    }
	    
	    // Teile den Namen durch Leerzeichen
	    String[] parts = withoutInitial.split("\\s+");
	    
	    // Gib den letzten Teil zurück (Nachnamen)
	    return parts[parts.length - 1];
	}

	/**
	 * Erstellt einen initialenbasierten Namen für die Suche
	 * 
	 * @param fullName Vollständiger Name (z.B. "R. Adam", "Thomas Sborn")
	 * @return Initial+basierten Namen (z.B. "r adam", "t sborn")
	 */
	private static String extractInitialBasedName(String fullName) {
	    if (fullName == null || fullName.isEmpty()) {
	        return "";
	    }
	    
	    // Normalisiere den Namen
	    String normalized = PlayerHelper.normalizeName(fullName);
	    
	    // Teile den Namen durch Leerzeichen
	    String[] parts = normalized.split("\\s+");
	    
	    if (parts.length < 2) {
	        return normalized;
	    }
	    
	    // Erstelle Initial+basierten Namen: erster Buchstabe + Nachname
	    String initial = parts[0].substring(0, 1);
	    String lastName = parts[parts.length - 1];
	    
	    return initial + " " + lastName;
	}

	/**
	 * Transliteriert einen Namen für verschiedene Schriftzeichen
	 * Konvertiert z.B. "İ" → "I", "ğ" → "g", "ş" → "s", "ç" → "c", "ö" → "o", "ü" → "u"
	 * 
	 * @param name Name mit potenziellen Transliterationsproblemen
	 * @return Transliterierter Name
	 */
	private static String transliterateName(String name) {
	    if (name == null || name.isEmpty()) {
	        return "";
	    }
	    
	    // Einfache Transliteration für häufige Probleme
	    String transliterated = name
	        .replace('İ', 'I')    // İ → I
	        .replace('ı', 'i')    // ı → i
	        .replace('ğ', 'g')    // ğ → g
	        .replace('ş', 's')    // ş → s
	        .replace('ç', 'c')    // ç → c
	        .replace('ö', 'o')    // ö → o
	        .replace('ü', 'u')    // ü → u
	        .replace('é', 'e')    // é → e
	        .replace('è', 'e')    // è → e
	        .replace('ê', 'e')    // ê → e
	        .replace('à', 'a')    // à → a
	        .replace('â', 'a')    // â → a
	        .replace('î', 'i')    // î → i
	        .replace('ô', 'o')    // ô → o
	        .replace('û', 'u')    // û → u
	        .replace('ë', 'e')    // ë → e
	        .replace('ï', 'i')    // ï → i
	        .replace('ü', 'u');   // ü → u (zweimal für Sicherheit)
	    
	    return transliterated;
	}
}