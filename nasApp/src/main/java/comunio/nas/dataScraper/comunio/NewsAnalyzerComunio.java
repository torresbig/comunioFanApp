package comunio.nas.dataScraper.comunio;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.TransferNewsEntry;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.user.User;
import comunio.nas.util.player.PlayerHelper;
import comunio.nas.dataVariable.Dates;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.enu.NewsArt;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analysiert News aus der Comunio-API und extrahiert Transfers, Bonuszahlungen,
 * Punktekorrekturen und Aufstellungen. Fügt sie der NewsManager-Datenbank hinzu
 * und aktualisiert das Player-zu-User-Mapping auf GitHub.
 */
public class NewsAnalyzerComunio {

	private static final Logger LOGGER = LogManager.getLogger(NewsAnalyzerComunio.class);

	/**
	 * Holt, verarbeitet und speichert neue Transfer-News sowie die "Elf des Tages".
	 * Die Suche beginnt ab dem letzten bekannten Abfragedatum
	 * (lastUpdates.getNews()).
	 * Nach der Abfrage wird das neueste gefundene Datum in lastUpdates gespeichert.
	 *
	 * @param newsManager     NewsManager-Instanz
	 * @param playerDBObject  JSONObject mit Spieler-Datenbank ("playerDB")
	 * @param playerToUserMap Mapping Spieler-ID -> User-ID
	 * @param notInLigaDbObj  JSONObject für nicht gefundene Spieler
	 * @param matchdayInfo    aktuelle Spieltagsinfo
	 * @param lastUpdates     letzte Aktualisierungszeitstempel (wird ggf.
	 *                        aktualisiert)
	 * @param user            aktueller Comunio-Benutzer
	 */
	public static void analyzeNews(NewsManager newsManager, JSONObject playerDBObject,
			Map<String, String> playerToUserMap, JSONObject notInLigaDbObj,
			MatchdayInfo matchdayInfo, LastUpdates lastUpdates, User user) {
		try {
			final ComunioDate MIN_DATE = new ComunioDate(Dates.getSeasonStart());
			// lastUpdates darf nicht null sein – falls doch, wird ein leerer Stand genommen
			if (lastUpdates == null) {
				lastUpdates = new LastUpdates();
			}
			final ComunioDate START_DATE = new ComunioDate(Date.from(lastUpdates.getNews()));

			// Ab diesem Datum (einschließlich) suchen wir
			ComunioDate searchDate = START_DATE.before(MIN_DATE) ? MIN_DATE : START_DATE;

			// Spieler-Datenbank auslesen
			JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
			if (playerDB == null) {
				LOGGER.log(Level.WARNING, "KEINE SPIELERDATENBANK vorhanden!");
				return;
			}

			// Vorbereitung
			List<TransferNewsEntry> transferNewsList = new ArrayList<>();
			Set<String> processedElfDesTages = newsManager.getNewsIdsForArt(NewsArt.ELFDESTAGES);

			int page = 0;
			int limit = 20;
			boolean hasMore = true;
			boolean stopBecauseOfOldDate = false;

			// Das neueste gefundene Datum während der Abfrage
			ComunioDate latestNewsDate = searchDate;

			LOGGER.info("Starte News-Abfrage ab (einschließlich): " + searchDate);

			while (hasMore && !stopBecauseOfOldDate) {
				String url = Urls.COM_NEWS(user) + "?group=true&originaltypes=true&start=" + page + "&limit=" + limit;
				JSONObject newsRoot;

				try {
					Connection connection = Jsoup.connect(url)
							.header("Accept", "application/json, text/plain, */*")
							.header("Authorization", "Bearer " + Login.getToken())
							.ignoreContentType(true);
					Document doc = connection.get();
					newsRoot = doc != null ? new JSONObject(doc.body().text()) : null;
				} catch (Exception e) {
					LOGGER.warning("Fehler beim Laden der News: " + e.getMessage());
					break;
				}

				if (newsRoot == null || !newsRoot.has("newsList")) {
					LOGGER.info("Keine weiteren News gefunden.");
					break;
				}

				JSONObject newsList = newsRoot.getJSONObject("newsList");
				JSONObject groups = newsList.optJSONObject("groups");
				if (groups == null) {
					LOGGER.info("Keine News-Gruppen gefunden.");
					break;
				}

				boolean foundOldDate = false;

				for (String dateKey : groups.keySet()) {
					ComunioDate newsDate = new ComunioDate(dateKey);
					if (newsDate.before(searchDate)) {
						if (foundOldDate == false) {
							LOGGER.info("Zu altes Datum (" + newsDate + ") gefunden. Stoppe nach dieser Seite.");
						}
						foundOldDate = true;
						continue;
					}

					// Merke das neueste Datum
					if (newsDate.after(latestNewsDate)) {
						latestNewsDate = newsDate;
					}

					JSONArray entries = groups.getJSONObject(dateKey).getJSONArray("entries");
					List<String> transferLogs = new ArrayList<>();
					List<String> elfLogs = new ArrayList<>();

					for (int i = 0; i < entries.length(); i++) {
						JSONObject entry = entries.getJSONObject(i);
						String type = entry.optString("type");
//						String title = entry.optString("title", "");

						if ("TRANSACTION_TRANSFER".equals(type)) {
							if (matchdayInfo != null && !matchdayInfo.askForTransfers(lastUpdates)) {
								LOGGER.info("Transfer-News am " + newsDate + " übersprungen, da Saison beendet.");
								continue;
							}
							String transactionId = String.valueOf(entry.optLong("id"));

							JSONObject message = entry.getJSONObject("message");
							for (String transferType : new String[] { "FROM_COMPUTER", "TO_COMPUTER",
									"BETWEEN_USERS" }) {
								if (!message.has(transferType))
									continue;
								JSONArray transfers = message.getJSONArray(transferType);
								for (int j = 0; j < transfers.length(); j++) {
									try {
										JSONObject transfer = transfers.getJSONObject(j);
										String playerId = String
												.valueOf(transfer.getJSONObject("tradable").getInt("id"));
										JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, playerId,
												notInLigaDbObj);
										String sellerId = String.valueOf(transfer.getJSONObject("from").getInt("id"));
										String seller = transfer.getJSONObject("from").getString("name");
										String buyerId = String.valueOf(transfer.getJSONObject("to").getInt("id"));
										String buyer = transfer.getJSONObject("to").getString("name");
										int price = transfer.getInt("price");
										int value = player != null ? player.getJSONObject("data").optInt("wert", 0) : 0;

										News news = News.getTransfer(sellerId, seller, buyerId, buyer, price, value,
												playerId, transfer.getJSONObject("tradable").getString("name"),
												newsDate, transactionId);
										transferLogs.add(newsDate + " - TRANSFER: "
												+ transfer.getJSONObject("tradable").getString("name")
												+ " von " + seller + " zu " + buyer);
										transferNewsList.add(new TransferNewsEntry(newsDate, playerId, buyerId, news));
									} catch (Exception e) {
										LOGGER.info("Fehler bei Transfer-Verarbeitung: " + e.getMessage());
									}
								}
							}
						} else if ("BEST_LINEUP".equals(type)) {
							String elfId = String.valueOf(entry.optLong("id"));
							if (processedElfDesTages.contains(elfId)) {
								continue;
							}
							processedElfDesTages.add(elfId);

							JSONObject lineUpMessage = entry.optJSONObject("message");
							if (lineUpMessage != null && lineUpMessage.has("lineup")) {
								JSONObject lineup = lineUpMessage.getJSONObject("lineup");
								String[] positions = { "keeper", "defender", "midfielder", "striker" };
								List<JSONObject> playerList = new ArrayList<>();
								for (String position : positions) {
									if (!lineup.has(position))
										continue;
									JSONArray playersInPosition = lineup.getJSONArray(position);
									for (int idxPlayer = 0; idxPlayer < playersInPosition.length(); idxPlayer++) {
										JSONObject elfPlayerData = new JSONObject();
										JSONObject playerFromLineup = playersInPosition.getJSONObject(idxPlayer);
										String playerId = String.valueOf(playerFromLineup.getInt("id"));

										JSONObject playerInDB = PlayerHelper.findPlayerByComunioId(playerDB, playerId,
												notInLigaDbObj);
										if (playerInDB != null) {
											JSONObject playerData = playerInDB.optJSONObject("data");
											if (playerData == null) {
												playerData = new JSONObject();
												playerInDB.put("data", playerData);
											}
											int elfCount = playerData.optInt("elfDesSpieltages", 0);
											String userID = PlayerHelper.getOwnerAtMatchday(playerId, matchdayInfo,
													newsManager.dbToJson(), playerToUserMap);
											elfPlayerData.put("playerName", playerInDB.getString("name"));
											elfPlayerData.put("playerId", playerInDB.getString("id"));
											elfPlayerData.put("position", position);
											elfPlayerData.put("punkte", playerFromLineup.getInt("points"));
											if (userID == null) {
												userID = "1";
											}
											elfPlayerData.put("owner", userID);
											playerList.add(elfPlayerData);
											elfCount++;
											playerData.put("elfDesSpieltages", elfCount);
										}
									}
								}
								News news = News.get11desTages(playerList, elfId, newsDate);
								newsManager.addNews(news, true);
							}
						}
					}

					if (!transferLogs.isEmpty()) {
						LOGGER.info("Transfers am " + newsDate + ":\n" + String.join("\n", transferLogs));
					}
					if (!elfLogs.isEmpty()) {
						LOGGER.info("Elf des Tages am " + newsDate + ":\n" + String.join("\n", elfLogs));
					}
				}

				if (foundOldDate)
					stopBecauseOfOldDate = true;
				hasMore = newsList.optBoolean("hasMore", false);
				page++;
			}

			// Transfers anwenden
			transferNewsList.sort(Comparator.comparing(TransferNewsEntry::getDate));
			for (TransferNewsEntry entry : transferNewsList) {
				String oldOwner = playerToUserMap.get(entry.getPlayerId());
				if (!Objects.equals(oldOwner, entry.getBuyerId())) {
					playerToUserMap.put(entry.getPlayerId(), entry.getBuyerId());
					GitHubUploader.mappingChanged = true;
					LOGGER.info("Mapping aktualisiert: Spieler " + entry.getPlayerId() + " -> User "
							+ entry.getBuyerId() + " am " + entry.getDate());
				}
				if (!newsManager.contains(entry.getNews())) {
					newsManager.addNews(entry.getNews(), true);
				} else {
					List<News> list = newsManager.getNewsByDate().get(entry.getNews().getDate());
					for (News nws : list) {
						if (nws.equals(entry.getNews())) {
							nws.setId(entry.getNews().getId());
						}
					}
				}
			}

			// Neuestes Datum in lastUpdates speichern (nur wenn neuer als bisher)
			Instant latestInstant = latestNewsDate.toInstant();
			if (latestInstant.isAfter(lastUpdates.getNews())) {
				lastUpdates.setNews(latestInstant);
				LOGGER.info("Neuestes Abfragedatum aktualisiert auf: " + latestNewsDate);
			}

		} catch (Exception e) {
			LOGGER.severe("Fehler in analyzeNews: " + e.getMessage());
		}
	}

	/**
	 * Gibt das letzte News-Datum als ComunioDate zurück.
	 */
	public static ComunioDate getLastNewsDateFromGitHub(JSONArray newsDB) {
		ComunioDate latest = null;
		for (int i = 0; i < newsDB.length(); i++) {
			JSONObject newsObj = newsDB.getJSONObject(i);
			if (newsObj.has("date")) {
				ComunioDate d = new ComunioDate(newsObj.getString("date"));
				if (latest == null || d.after(latest)) {
					latest = d;
				}
			}
		}
		return latest;
	}

	/**
	 * Gibt das Datum der letzten TRANSFER-News als ComunioDate zurück. Falls keine
	 * Transfer-News gefunden wird, gibt die Methode null zurück.
	 * 
	 * Durchsuche dazu die News.json von GitHub nach allen News-Einträgen mit "art":
	 * "TRANSFER" und ermittle das jeweils zugehörige Datum. Liefere das neueste
	 * Datum zurück.
	 * 
	 * @return ComunioDate des letzten Transfers oder null, wenn keine vorhanden.
	 */
	public static ComunioDate getLastTransferNewsDateFromGitHub(JSONArray newsDB) {

		ComunioDate latest = null;
		for (int i = 0; i < newsDB.length(); i++) {
			JSONObject dateEntry = newsDB.getJSONObject(i);
			if (!dateEntry.has("date") || !dateEntry.has("news"))
				continue;
			ComunioDate date = new ComunioDate(dateEntry.getString("date"));
			JSONArray newsList = dateEntry.getJSONArray("news");
			for (int j = 0; j < newsList.length(); j++) {
				JSONObject newsObj = newsList.getJSONObject(j);
				if (newsObj.has("art") && "TRANSFER".equals(newsObj.getString("art"))) {
					if (latest == null || date.after(latest)) {
						latest = date;
					}
				}
			}
		}
		return latest;
	}

}
