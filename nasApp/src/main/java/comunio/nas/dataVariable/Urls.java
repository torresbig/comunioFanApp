package comunio.nas.dataVariable;

import comunio.nas.dataScraper.comunio.ComunioDataUpdater;
import comunio.nas.objects.user.User;

public class Urls {

	/** API-URL für die Benutzerdaten, benötigt wird die userID */
	public static final String COM_API(User user) {
		return "https://www.comunio.de/api/communities/" + ComunioDataUpdater.community.getId() + "/users/" + user.getId();
	}

	public static final String COM_NEWS(User user) {
		return COM_API(user) + "/news";
	}

	public static final String COM_CLUB = "https://www.comunio.de/api/clubs/";
	public static final String COM_MATCHDAY = "https://www.comunio.de/api/matchdays/current";

	public static String COM_PLAYER_FOTO(String playerID) {
		return "https://www.comunio.de/api/players/" + playerID + "/photo";
	}

	/**
	 * Man benötigt noch die userID
	 */
	public static final String COM_USERDATA = "https://www.comunio.de/api/users/";

	/** GitHub-URLs für die Datenbanken */
	public static final String PLAYER_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("SpielerdatenbankNeutralJson.txt");
	public static final String CLUB_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("VereinsdatenbankJson.txt");
	public static final String MARKET_VALUE_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("MarktwerteJson.txt");
	public static final String NEWS_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("News.json");
	public static final String USER_TO_PLAYER_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("PlayerToUserMap_" + ComunioDataUpdater.community.getId() + ".txt");
	public static final String USER_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("UserdatenbankJson_" + ComunioDataUpdater.community.getId() + ".txt");
	public static final String NOTINLIGA_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("NotInLigaDB.json");
	public static final String TRANSFERMARKT_LIST = ComunioDataUpdater.uld.getCompleteGitProjectPath("TransfermarktListe.json");
	public static final String MATCHDAYDATA_LIST = ComunioDataUpdater.uld.getCompleteGitProjectPath("MatchdayDataList.json");
	public static final String LASTUPDATES_LIST = ComunioDataUpdater.uld.getCompleteGitProjectPath("LastUpdatesList.json");
	public static final String INJURIES_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("InjuriesDB.json"); // wird in zukunft ausgelagert in extra DB
	public static final String POINTS_DB_URL = ComunioDataUpdater.uld.getCompleteGitProjectPath("PointsDB.json");

	/** spielersuche auf Transfermarkt.de */
//	public static final String TMDE_PLAYERSEARCH = "https://www.transfermarkt.de/schnellsuche/ergebnis/schnellsuche?query=";
	public static String TMDE_PLAYERSEARCH_WITH_SITES(int site, String playerName) {
		return "https://www.transfermarkt.de/schnellsuche/ergebnis/schnellsuche?Spieler_page=" + site + "&query=" + playerName;
	}

	/** Ligainsider */
	public static final String LIGAINSIDER_URL = "https://www.ligainsider.de/bundesliga/noten/";

	public static String getFilename(String url) {
		String[] splits = url.split("/");
		return splits[splits.length - 1];
	}

	public static String getFilePathForGit(String url) {
		String[] splits = url.split("/");
		return splits[splits.length - 2] + "/" + splits[splits.length - 1];
	}

}
