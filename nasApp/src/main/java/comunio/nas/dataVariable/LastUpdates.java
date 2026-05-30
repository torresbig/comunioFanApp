package comunio.nas.dataVariable;

import java.time.Instant;
import java.util.logging.Logger;

import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.objects.helper.LogManager;

public class LastUpdates {

	private static final Logger LOGGER = LogManager.getLogger(LastUpdates.class);

	private Instant playerDbFull;
	private Instant playerDbShort;
	private Instant matchdayInfo;
	private Instant news;
	private Instant users;
	private Instant transfermarktList;
	private Instant fussballdatenDe;
	private Instant transfermarktDe;
	private Instant ligainsider;
	private Instant comAnalystics;
	private Instant notInLigaDb;
	private Instant playerStatus;
	private Instant seasonStart;
	private Instant clubDb;

	public LastUpdates() {

	}

	public void fromJson(JSONObject json) {
		this.playerDbFull = json.has("playerDbFull") ? Instant.ofEpochMilli(json.getLong("playerDbFull")) : null;
		this.playerDbShort = json.has("playerDbShort") ? Instant.ofEpochMilli(json.getLong("playerDbShort")) : null;
		this.matchdayInfo = json.has("matchdayInfo") ? Instant.ofEpochMilli(json.getLong("matchdayInfo")) : null;
		this.news = json.has("news") ? Instant.ofEpochMilli(json.getLong("news")) : null;
		this.users = json.has("users") ? Instant.ofEpochMilli(json.getLong("users")) : null;
		this.transfermarktList = json.has("transfermarktList") ? Instant.ofEpochMilli(json.getLong("transfermarktList"))
				: null;
		this.fussballdatenDe = json.has("fussballdatenDe") ? Instant.ofEpochMilli(json.getLong("fussballdatenDe"))
				: null;
		this.transfermarktDe = json.has("transfermarktDe") ? Instant.ofEpochMilli(json.getLong("transfermarktDe"))
				: null;
		this.ligainsider = json.has("ligainsider") ? Instant.ofEpochMilli(json.getLong("ligainsider")) : null;
		this.comAnalystics = json.has("comAnalystics") ? Instant.ofEpochMilli(json.getLong("comAnalystics")) : null;
		this.notInLigaDb = json.has("notInLigaDb") ? Instant.ofEpochMilli(json.getLong("notInLigaDb")) : null;
		this.playerStatus = json.has("playerStatus") ? Instant.ofEpochMilli(json.getLong("playerStatus")) : null;
		this.seasonStart = json.has("seasonStart") ? Instant.ofEpochMilli(json.getLong("seasonStart")) : null;
		this.clubDb = json.has("clubDb") ? Instant.ofEpochMilli(json.getLong("clubDb")) : null;

		if (this.seasonStart != null && this.seasonStart.toEpochMilli() > 0) {
			Dates.setSeasonStart(this.seasonStart);
			LOGGER.warning("seasonStart ist null in LastUpdates. Bitte überprüfen Sie die JSON-Daten.");
		}

	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		if (this.playerDbFull != null) {
			json.put("playerDbFull", this.playerDbFull.toEpochMilli());
		}
		if (this.playerDbShort != null) {
			json.put("playerDbShort", this.playerDbShort.toEpochMilli());
		}
		if (this.matchdayInfo != null) {
			json.put("matchdayInfo", this.matchdayInfo.toEpochMilli());
		}
		if (this.news != null) {
			json.put("news", this.news.toEpochMilli());
		}
		if (this.users != null) {
			json.put("users", this.users.toEpochMilli());
		}
		if (this.transfermarktList != null) {
			json.put("transfermarktList", this.transfermarktList.toEpochMilli());
		}
		if (this.fussballdatenDe != null) {
			json.put("fussballdatenDe", this.fussballdatenDe.toEpochMilli());
		}
		if (this.transfermarktDe != null) {
			json.put("transfermarktDe", this.transfermarktDe.toEpochMilli());
		}
		if (this.ligainsider != null) {
			json.put("ligainsider", this.ligainsider.toEpochMilli());
		}
		if (this.comAnalystics != null) {
			json.put("comAnalystics", this.comAnalystics.toEpochMilli());
		}
		if (this.notInLigaDb != null) {
			json.put("notInLigaDb", this.notInLigaDb.toEpochMilli());
		}
		if (this.playerStatus != null) {
			json.put("playerStatus", this.playerStatus.toEpochMilli());
		}
		if (this.seasonStart != null) {
			json.put("seasonStart", this.seasonStart.toEpochMilli());
		}
		if (this.clubDb != null) {
			json.put("clubDb", this.clubDb.toEpochMilli());
		}
		return json;

	}

	public void setSeasonStart(Instant timestamp) {
		this.seasonStart = timestamp;
	}

	public Instant getSeasonStart() {
		return seasonStart;
	}

	public Instant getClubDb() {
		return clubDb;
	}

	public void setClubDb(Instant clubDb) {
		this.clubDb = clubDb;
	}

	public Instant getPlayerDbFull() {
		return playerDbFull;
	}

	public Instant getPlayerDbShort() {
		return playerDbShort;
	}

	public Instant getMatchdayInfo() {
		return matchdayInfo;
	}

	public Instant getNews() {
		return news;
	}

	public Instant getUsers() {
		return users;
	}

	public Instant getTransfermarktList() {
		return transfermarktList;
	}

	public Instant getFussballdatenDe() {
		return fussballdatenDe;
	}

	public Instant getTransfermarktDe() {
		return transfermarktDe;
	}

	public Instant getLigainsider() {
		return ligainsider;
	}

	public Instant getComAnalystics() {
		return comAnalystics;
	}

	public Instant getNotInLigaDb() {
		return notInLigaDb;
	}

	public Instant getPlayerStatus() {
		return playerStatus;
	}

	public void setPlayerDbFull(Instant playerDbFull) {
		this.playerDbFull = playerDbFull;
	}

	public void setPlayerDbShort(Instant playerDbShort) {
		this.playerDbShort = playerDbShort;
	}

	public void setMatchdayInfo(Instant matchdayInfo) {
		this.matchdayInfo = matchdayInfo;
	}

	public void setNews(Instant news) {
		this.news = news;
	}

	public void setUsers(Instant users) {
		this.users = users;
	}

	public void setTransfermarktList(Instant transfermarktList) {
		this.transfermarktList = transfermarktList;
	}

	public void setFussballdatenDe(Instant fussballdatenDe) {
		this.fussballdatenDe = fussballdatenDe;
	}

	public void setTransfermarktDe(Instant transfermarktDe) {
		this.transfermarktDe = transfermarktDe;
	}

	public void setLigainsider(Instant ligainsider) {
		this.ligainsider = ligainsider;
	}

	public void setComAnalystics(Instant comAnalystics) {
		this.comAnalystics = comAnalystics;
	}

	public void setNotInLigaDb(Instant notInLigaDb) {
		this.notInLigaDb = notInLigaDb;
	}

	public void setPlayerStatus(Instant playerStatus) {
		this.playerStatus = playerStatus;
	}

	/**
	 * Prüft, ob der Saisonübergangszeitpunkt (seasonStart in LastUpdates) vor dem
	 * Ende
	 * des letzten Spieltags liegt, sofern die Saison bereits vorbei ist.
	 *
	 * @param lastUpdates  die aktuellen Saison-Updates (enthält seasonStart)
	 * @param matchdayInfo Informationen zum aktuellen Spieltag (kann null sein)
	 * @return true, wenn:
	 *         - matchdayInfo null ist (Standard-Fallback)
	 *         - seasonStart null ist (wird als "davor" gewertet)
	 *         - die Saison vorbei ist und seasonStart vor dem letzten Anstoß liegt;
	 *         false sonst
	 */
	public static boolean isSeasonTransitionBeforeLastMatchday(LastUpdates lastUpdates, MatchdayInfo matchdayInfo) {
		if (matchdayInfo == null) {
			return false; // Kein gültiges MatchdayInfo-Objekt oder Saison bereits vorbei
		}
		Instant seasonTransmission = lastUpdates.getSeasonStart();

		if (seasonTransmission == null) {
			return true; // oder true, je nachdem, wie du es handhaben möchtest
		}

		if (matchdayInfo.isSeasonOver(lastUpdates)) {
			Instant lastMatchOver = matchdayInfo.getLatestKickoff().toInstant();
			if (seasonTransmission.isBefore(lastMatchOver)) {
				return true; // Saisonübergang liegt vor dem Ende der letzten Spieltag
			} else {
				return false; // Saisonübergang liegt nach dem Ende der letzten Spieltag
			}
		}
		return false; // Saisonübergang liegt nicht vor dem Ende der letzten Spieltag
	}

	/**
	 * Prüft, ob die Anwendung sich in einem Zustand "zwischen den Saisons"
	 * befindet,
	 * also der Saisonübergangszeitpunkt (seasonStart) bereits verstrichen ist, aber
	 * die neue Saison noch nicht begonnen hat (d.h. seasonStart liegt nach dem Ende
	 * des letzten Spieltags der alten Saison).
	 *
	 * @param lastUpdates  die aktuellen Saison-Updates (enthält seasonStart)
	 * @param matchdayInfo Informationen zum aktuellen Spieltag (kann null sein)
	 * @return true, wenn:
	 *         - die Saison vorbei ist UND
	 *         - seasonStart nicht null ist UND
	 *         - seasonStart nach dem letzten Anstoß des letzten Spieltags liegt;
	 *         false sonst (z.B. bei null-Werten oder wenn der Übergang noch
	 *         aussteht)
	 */

	public static boolean isStuckBetweenTheSeasons(LastUpdates lastUpdates, MatchdayInfo matchdayInfo) {
		if (matchdayInfo == null) {
			return false; // Kein gültiges MatchdayInfo-Objekt oder Saison bereits vorbei
		}
		Instant seasonTransmission = lastUpdates.getSeasonStart();

		if (seasonTransmission == null) {
			return false;
		}

		if (matchdayInfo.isSeasonOver(lastUpdates)) {
			Instant lastMatchOver = matchdayInfo.getLatestKickoff().toInstant();
			if (seasonTransmission.isBefore(lastMatchOver)) {
				return false; // Saisonübergang liegt vor dem Ende der letzten Spieltag
			} else {
				return true; // Saisonübergang liegt nach dem Ende der letzten Spieltag
			}
		}
		return false; // Saisonübergang liegt nicht vor dem Ende der letzten Spieltag
	}
}