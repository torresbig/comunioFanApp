package comunio.nas.dataScraper.wettbasis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.Urls;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.InjuryAndBlockedData;
import comunio.nas.objects.wettbasis.WettbasisClubObject;

public class WettbasisInjurieAndBannedPlayers {

	private static final Logger LOGGER = LogManager.getLogger(WettbasisInjurieAndBannedPlayers.class);
	private static final String SOURCE = "wettbasis";
	private static Map<String, WettbasisClubObject> lastClubMapping = new HashMap<>();

	/**
	 * Parst verletzte/gesperrte Spieler von Wettbasis.
	 * <p>
	 * Rückgabeformat ist identisch zur Ligainsider-Quelle:
	 * {@code Map<ClubName, List<InjuryAndBlockedData>>}.
	 */
	public static Map<String, List<InjuryAndBlockedData>> parseVerletzteUndGesperrte() {
		Map<String, List<InjuryAndBlockedData>> result = new HashMap<>();
		lastClubMapping = new HashMap<>();
		String url = Urls.WETTBASIS_INJURY;
		LOGGER.info("Lade verletzte und gesperrte Spieler von: " + url);
		try {
			Document doc = Jsoup.connect(url).get();
			Elements teams = doc.select(".bsc-injuries-and-suspensions-league .team-container");

			for (Element team : teams) {
				String clubName = team.select(".heading .internal-link").first() != null ? team.select(".heading .internal-link").first().text().trim() : "";
				if (clubName.isBlank()) {
					continue;
				}

				String logoUrl = "";
				Element logoImg = team.select(".participant .logo img").first();
				if (logoImg != null) {
					logoUrl = logoImg.attr("data-lazy-src");
					if (logoUrl == null || logoUrl.isBlank()) {
						logoUrl = logoImg.attr("src");
					}
				}
				String clubExternalId = extractClubExternalId(logoUrl);
				lastClubMapping.put(clubName, new WettbasisClubObject(clubName, clubExternalId, logoUrl));

				List<InjuryAndBlockedData> clubList = new ArrayList<>();
				Elements rows = team.select(".row-wrapper");
				for (Element row : rows) {
					String playerName = row.select(".el-player .name").text().trim();
					if (playerName.isBlank()) {
						continue;
					}

					String reason = row.select(".el-player .reason").text().trim();
					String expectedReturn = row.select(".column-end .el-return").text().trim();
					boolean suspension = !row.select(".injury-or-suspension-icon-wrapper .dot.exclamation").isEmpty() || isSuspensionReason(reason);

					InjuryAndBlockedData data = new InjuryAndBlockedData();
					data.setClub(clubName);
					data.setPlayerName(playerName);
					data.setReason(reason);
					data.setSinceString(expectedReturn);
					data.setLastNewsText(expectedReturn.isBlank() ? "Wettbasis" : "Rückkehr: " + expectedReturn);
					data.setLastNewsLink(url);
					data.setQuelle(SOURCE);
					data.setStatus(suspension ? SpielerStatus.GESPERRT : SpielerStatus.VERLETZT);
					clubList.add(data);
				}

				if (!clubList.isEmpty()) {
					result.put(clubName, clubList);
				}
			}
		} catch (Exception e) {
			LOGGER.warning("Fehler beim abfragen der Wettbasiswebsite - VERLETZE & GESPERRTE Spieler -->" + url + " << FEHLER: " + e.getMessage());
		}
		return result;
	}

	/**
	 * Liefert das zuletzt geparste Club-Mapping (Name, externe ID, Logo-Link).
	 */
	public static Map<String, WettbasisClubObject> getLastClubMapping() {
		return new HashMap<>(lastClubMapping);
	}

	/**
	 * Erkennt Sperren über Reason-Texte, falls kein explizites Sperren-Icon erkannt wurde.
	 */
	private static boolean isSuspensionReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return false;
		}
		String normalized = reason.toLowerCase();
		return normalized.contains("karte") || normalized.contains("gesperrt") || normalized.contains("sperre");
	}

	/**
	 * Extrahiert die externe Club-ID aus dem Wettbasis-Logo-URL-Segment.
	 */
	private static String extractClubExternalId(String logoUrl) {
		if (logoUrl == null || logoUrl.isBlank()) {
			return "";
		}
		int marker = logoUrl.lastIndexOf("/logotype/");
		if (marker < 0) {
			return "";
		}
		String tail = logoUrl.substring(marker + "/logotype/".length());
		int dotIndex = tail.indexOf('.');
		if (dotIndex > 0) {
			return tail.substring(0, dotIndex);
		}
		return tail;
	}
}
