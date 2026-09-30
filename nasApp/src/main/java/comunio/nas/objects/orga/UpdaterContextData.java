package comunio.nas.objects.orga;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.LineupParser;
import comunio.nas.dataScraper.ligainsider.LigainsiderInjurieAndBannedPlayers;
import comunio.nas.dataScraper.ligainsider.LigainsiderPossibleFormation;
import comunio.nas.dataScraper.tools.PlayerpointsToPlayerObject;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.error.ErrorsContainer;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.ComunioTransfermarketContainer;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.club.ClubDbContainer;
import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.espn.EspnClubContainer;
import comunio.nas.objects.espn.EspnPlayerContainer;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;
import comunio.nas.util.LoadJSONfromFile;

public class UpdaterContextData {

	private static final Logger LOGGER = LogManager.getLogger(UpdaterContextData.class);

	private JSONObject matchdayInfoList;
	private ClubDbContainer clubDbContainer;
	private LigainsiderInjurieAndBannedPlayers injuryDB;
	private JSONObject playerDBObject;
	private JSONObject pointsDB;
	private JSONArray marketValueDB;
	private NewsManager newsManager;
	private Map<String, User> userMap;
	private LineupParser lineupParser;
	private JSONObject notInligaDBObj;
	private ComunioTransfermarketContainer comunioTransfermarktContainer;
	private Map<String, String> playerToUserMap;
//	private StatusManager statusManager;
	public static Set<String> ownerList = new HashSet<>();


	// Ligainsider
	private LigainsiderPossibleFormation possibleFormationMap;

	// ESPN-Daten
	private EspnClubContainer espnClubMappingContainer;
	private EspnPlayerContainer espnPlayerMappingContainer;

	public UpdaterContextData() {

	}

	public UpdaterContextData loadAllData(LastUpdates lastUpdates, User user) throws Exception {
		new UpdaterContextData();

		LOGGER.info("Lade ErrorDb von GitHub");
		try {
			ComunioDataUpdater.errorDb.fromJson(LoadJSONfromFile.loadJsonObjectFromUrl(Urls.ERROR_DB_URL));
		} catch (Exception e) {
			LOGGER.warning("Fehler beim Laden der ErrorDb von GitHub: " + e.getMessage());
			ComunioDataUpdater.errorDb = new ErrorsContainer();
		}

		LOGGER.info("Lade LastUpdates Liste von GitHub");
		JSONObject lastUpdatesList = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.LASTUPDATES_LIST_URL);
		lastUpdates.fromJson(lastUpdatesList);

		LOGGER.info("Lade Matchday Liste von GitHub");
		this.matchdayInfoList = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.MATCHDAYDATA_LIST_URL);

		LOGGER.info("Lade Vereinsdaten von GitHub");
		this.clubDbContainer = ClubDbContainer.fromJSON(LoadJSONfromFile.loadJsonArrayFromUrl(Urls.CLUB_DB_URL));

		LOGGER.info("Lade Verletzungen von GitHub");
		this.injuryDB = LigainsiderInjurieAndBannedPlayers.fromJSON(LoadJSONfromFile.loadJsonFromUrl(Urls.INJURIES_DB_URL));

		LOGGER.info("Lade Spielerdatenbank von GitHub");
		this.playerDBObject = LoadJSONfromFile.loadJsonObjectWithPlayerArrayFromUrl(Urls.PLAYER_DB_URL);

		LOGGER.info("Lade Playerpoints von GitHub");
		this.pointsDB = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.POINTS_DB_URL);
//		this.pointsDB = JsonCleanerUtil.reduceToKeyAndValue(this.pointsDB);
		PlayerpointsToPlayerObject.putPointsToPlayerObject(this.pointsDB, this.playerDBObject);

		LOGGER.info("Lade Marktwertdatenbank von GitHub");
		this.marketValueDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.MARKET_VALUE_DB_URL);

		LOGGER.info("Lade Newsdatenbank von GitHub");
		JSONObject newsDbObjcet = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.NEWS_DB_URL);
		this.newsManager = NewsManager.fromJsonObject(newsDbObjcet);

		LOGGER.info("Lade Userdatenbank von GitHub");
		this.userMap = getUserMap();

		LOGGER.info("Lade UserLineup von GitHub");
		JSONObject userLineupJson = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.USER_LINEUPS);
		this.lineupParser = new LineupParser();
		this.lineupParser.loadMapFromJsonString(userLineupJson);

		LOGGER.info("Lade NotInLiga-PlayerDB von GitHub");
		this.notInligaDBObj = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.NOTINLIGA_DB_URL);

		LOGGER.info("Lade TransfermarktListe von GitHub");
		this.comunioTransfermarktContainer = ComunioTransfermarketContainer.fromJSON(LoadJSONfromFile.loadJsonFromUrl(Urls.TRANSFERMARKT_LIST_URL));

		LOGGER.info("Lade Player to User Map");
		this.playerToUserMap = GitHubUploader.downloadPlayerToUserMap(Urls.USER_TO_PLAYER_URL);

		// ESPN-Mappings laden (Club + Player)
		LOGGER.info("Lade ESPN Club-Mapping von GitHub");
		JSONObject espnClubMapping = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.ESPN_CLUB_MAPPING_URL);
		this.espnClubMappingContainer = EspnClubContainer.fromJson(espnClubMapping);

		LOGGER.info("Lade ESPN Player-Mapping von GitHub");
		JSONObject espnPlayerMapping = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.ESPN_PLAYER_MAPPING_URL);
		this.espnPlayerMappingContainer = EspnPlayerContainer.fromJson(espnPlayerMapping);

		LOGGER.info("Mögliche Aufstellung von GitHub");
		JSONObject possibleFormation = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.POSSIBLE_FORMATIOKN);
		this.possibleFormationMap = new LigainsiderPossibleFormation(possibleFormation);

		setOwnerList(this.userMap.keySet());

		return this;
	}
	
	
	public void uploadAllData(UpdaterContextData ctx, LastUpdates lastUpdates) {
	
		LOGGER.info("Lade aktualisierte Marktwertdatenbank auf GitHub hoch");
		GitHubUploader.uploadMarketValueDatabase(this.marketValueDB);
		
		LOGGER.info("Lade aktualisierte Verletzten-Datenbank (injuryDB) auf GitHub hoch");
		JSONObject inj = this.getInjuryDB().toJSON(); 
		GitHubUploader.uploadToGitHub(Urls.INJURIES_DB_URL,inj);

		LOGGER.info("Lade aktualisierte Punkte-Datenbank (pointsDB) auf GitHub hoch");
		PlayerpointsToPlayerObject.getPointsArrayFromAllPlayer(this.pointsDB, this.playerDBObject);
		GitHubUploader.uploadPlayerPoints(this.pointsDB);

		LOGGER.info("Lade aktualisierte Spielerdatenbank auf GitHub hoch");
		GitHubUploader.uploadPlayerDatabase(this.playerDBObject);

		LOGGER.info("Lade aktualisierte PlayerToUserMap auf GitHub hoch");
		GitHubUploader.uploadPlayerToUserMap(this.playerToUserMap);

		LOGGER.info("Lade aktualisierte Userdatenbank auf GitHub hoch");
		GitHubUploader.uploadUserDatabase(userMapToJSONArray(this.userMap));

		LOGGER.info("Lade aktualisierte UserLineups auf GitHub hoch");
		GitHubUploader.uploadUserLineups(this.lineupParser.getFinalJSONObject());

		LOGGER.info("Lade aktualisierte TransfermarktListe auf GitHub hoch");
		JSONObject comTmObject = this.comunioTransfermarktContainer.toJSON();
		GitHubUploader.uploadTransfermarktListe(comTmObject);

		LOGGER.info("Lade aktualisierte MatchdayInfo auf GitHub hoch");
		GitHubUploader.uploadMatchdayInfoListe(this.matchdayInfoList);

		LOGGER.info("Lade aktualisierte LastUpdates auf GitHub hoch");
		GitHubUploader.uploadLastUpdateListe(lastUpdates.toJson());

		LOGGER.info("Lade aktualisierte NotInLigaPlayerDB auf GitHub hoch");
		GitHubUploader.uploadToGitHub(Urls.NOTINLIGA_DB_URL, this.notInligaDBObj);

		LOGGER.info("Lade aktualisierte ClubDB auf GitHub hoch");
		GitHubUploader.uploadClubsDatabase(this.clubDbContainer.toJSON());

		LOGGER.info("Lade aktualisierte ErrorDb.json auf GitHub hoch");
		GitHubUploader.uploadToGitHub(Urls.ERROR_DB_URL, ComunioDataUpdater.errorDb.toJson());

		LOGGER.info("Lade aktualisierte News auf GitHub hoch");
		JSONObject newsDbObjcet = this.newsManager.objectToJson();
		GitHubUploader.uploadNews(newsDbObjcet);

		// ESPN-Mappings hochladen (falls vorhanden)
		LOGGER.info("Lade ESPN Club-Mapping auf GitHub hoch");
		GitHubUploader.uploadEspnClubMapping(this.espnClubMappingContainer.toJson());

		LOGGER.info("Lade ESPN Player-Mapping auf GitHub hoch");
		GitHubUploader.uploadEspnPlayerMapping(this.espnPlayerMappingContainer.toJson());
		
		LOGGER.info("Lade Mögliche Aufstellungen auf GitHub hoch");
		GitHubUploader.uploadPossibleFormationMapping(this.possibleFormationMap.toJSON());

		
	}

	public Map<String, User> getUserMap() {
		if (this.userMap == null) {
			this.userMap = new HashMap<>();
		}
		try {
			JSONArray userDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.USER_DB_URL);
			for (int i = 0; i < userDB.length(); i++) {
				JSONObject userJson = userDB.getJSONObject(i);
				User u = User.fromJson(userJson);
				this.userMap.put(u.getId(), u);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Laden der Userdatenbank: " + e.getMessage(), e);
		}
		return userMap;
	}
	
	private JSONArray userMapToJSONArray(Map<String, User> userMap) {
		JSONArray userArray = new JSONArray();
		for (User user : userMap.values()) {
			userArray.put(user.toJson());
		}
		return userArray;
	}

	public JSONObject getMatchdayInfoList() {
		return matchdayInfoList;
	}

	public void setMatchdayInfoList(JSONObject matchdayInfoList) {
		this.matchdayInfoList = matchdayInfoList;
	}

	public ClubDbContainer getClubDbContainer() {
		return clubDbContainer;
	}
	
	public Map<String, ClubObject> getClubDbMap() {
		if( this.clubDbContainer != null &&  this.clubDbContainer.getClubDb() != null) {
			return this.clubDbContainer.getClubDb();
		}
		return null;
	}

	public void setClubDbContainer(ClubDbContainer clubDbContainer) {
		this.clubDbContainer = clubDbContainer;
	}

	public LigainsiderInjurieAndBannedPlayers getInjuryDB() {
		return injuryDB;
	}

	public void setInjuryDB(LigainsiderInjurieAndBannedPlayers injuryDB) {
		this.injuryDB = injuryDB;
	}

	public JSONObject getPlayerDBObject() {
		return playerDBObject;
	}
	
	public JSONArray getPlayerDBArray() {
		if(this.playerDBObject != null && this.playerDBObject.has("playerDB")) {
			return this.playerDBObject.getJSONArray("playerDB");
		}
		return null;
	}

	public void setPlayerDBObject(JSONObject playerDBObject) {
		this.playerDBObject = playerDBObject;
	}

	public JSONObject getPointsDB() {
		return pointsDB;
	}

	public void setPointsDB(JSONObject pointsDB) {
		this.pointsDB = pointsDB;
	}

	public JSONArray getMarketValueDB() {
		return marketValueDB;
	}

	public void setMarketValueDB(JSONArray marketValueDB) {
		this.marketValueDB = marketValueDB;
	}

	public NewsManager getNewsManager() {
		return newsManager;
	}

	public void setNewsManager(NewsManager newsManager) {
		this.newsManager = newsManager;
	}

	public LineupParser getLineupParser() {
		return lineupParser;
	}

	public void setLineupParser(LineupParser lineupParser) {
		this.lineupParser = lineupParser;
	}

	public JSONObject getNotInligaDBObj() {
		return notInligaDBObj;
	}

	public void setNotInligaDBObj(JSONObject notInligaDBObj) {
		this.notInligaDBObj = notInligaDBObj;
	}

	public ComunioTransfermarketContainer getComunioTransfermarktContainer() {
		return comunioTransfermarktContainer;
	}

	public void setComunioTransfermarktContainer(ComunioTransfermarketContainer comunioTransfermarktContainer) {
		this.comunioTransfermarktContainer = comunioTransfermarktContainer;
	}

	public Map<String, String> getPlayerToUserMap() {
		return playerToUserMap;
	}

	public void setPlayerToUserMap(Map<String, String> playerToUserMap) {
		this.playerToUserMap = playerToUserMap;
	}

//	public StatusManager getStatusManager() {
//		return statusManager;
//	}
//
//	public void setStatusManager(StatusManager statusManager) {
//		this.statusManager = statusManager;
//	}

	public LigainsiderPossibleFormation getPossibleFormationMap() {
		return possibleFormationMap;
	}

	public void setPossibleFormationMap(LigainsiderPossibleFormation possibleFormationMap) {
		this.possibleFormationMap = possibleFormationMap;
	}

	public EspnClubContainer getEspnClubMappingContainer() {
		return espnClubMappingContainer;
	}
	
	

	public void setEspnClubMappingContainer(EspnClubContainer espnClubMappingContainer) {
		this.espnClubMappingContainer = espnClubMappingContainer;
	}

	public EspnPlayerContainer getEspnPlayerMappingContainer() {
		return espnPlayerMappingContainer;
	}

	public void setEspnPlayerMappingContainer(EspnPlayerContainer espnPlayerMappingContainer) {
		this.espnPlayerMappingContainer = espnPlayerMappingContainer;
	}

	public void setUserMap(Map<String, User> userMap) {
		this.userMap = userMap;
	}

	public Set<String> getOwnerList() {
		return ownerList;
	}

	public void setOwnerList(Set<String> ownerList) {
		UpdaterContextData.ownerList = ownerList;
	}

}