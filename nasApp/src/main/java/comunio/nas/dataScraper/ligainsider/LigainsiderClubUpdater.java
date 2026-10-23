package comunio.nas.dataScraper.ligainsider;

import java.util.Map;
import java.util.logging.Logger;
import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.LigainsiderClubObject;
import comunio.nas.util.ClubMapper;

public class LigainsiderClubUpdater {
	private static final Logger LOGGER = LogManager.getLogger(LigainsiderClubParser.class);

	public static Map<String, ClubObject> updateLigainsiderClubDate( Map<String, ClubObject> clubDb) {

		if (!allClubDataAvailable(clubDb)) {

			Map<String, LigainsiderClubObject> extractedClubs = LigainsiderClubParser.parseAndMapLigaInsider();

			for (Map.Entry<String, LigainsiderClubObject> entry : extractedClubs.entrySet()) {
				LigainsiderClubObject clubObj = entry.getValue();
				ClubObject matchingComunioClub = ClubMapper.getComunioClubObjectFromName(clubObj.getName(), clubDb);
				matchingComunioClub.setLigainsiderClub(clubObj);
			}
		}
		return clubDb;
	}

	private static boolean allClubDataAvailable( Map<String, ClubObject> clubDb) {
		for (Map.Entry<String, ClubObject> entry : clubDb.entrySet()) {
			ClubObject clubo = entry.getValue();
			if (clubo.getLigainsiderClub() == null ) {
				return false;
			}
			
			if (clubo.getLigainsiderClub().getLink() == null || clubo.getLigainsiderClub().getId() == null || clubo.getLigainsiderClub().getName() == null || clubo.getLigainsiderClub().getLink().isBlank() || clubo.getLigainsiderClub().getId().isBlank() || clubo.getLigainsiderClub().getName().isBlank()) {
				return false;
			}
		}
		return true;
	}

}
