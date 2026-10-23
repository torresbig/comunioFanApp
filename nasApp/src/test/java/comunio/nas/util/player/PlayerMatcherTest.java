package comunio.nas.util.player;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import comunio.nas.objects.club.ClubObject;

class PlayerMatcherTest {

    @Test
    void shouldMatchExactAndNormalizedNames() {
        JSONArray playerDb = new JSONArray();
        JSONObject dbPlayer = new JSONObject();
        dbPlayer.put("name", "Thomas Müller");
        dbPlayer.put("id", "101");
        JSONObject data = new JSONObject();
        data.put("verein", "42");
        data.put("possibleNames", new JSONArray(List.of("Thomas Mueller", "T. Müller", "Thomas-Peter Müller")));
        dbPlayer.put("data", data);
        playerDb.put(dbPlayer);

        Map<String, ClubObject> clubDb = new HashMap<>();
        ClubObject club = new ClubObject();
        club.setId("42");
        club.setName("FC Bayern München");
        clubDb.put("42", club);

        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "FC Bayern München", Set.of("Thomas-Peter Müller")));
        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "FC Bayern München", Set.of("T. Müller")));
        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "FC Bayern München", Set.of("Thomas Mueller")));
    }

    @Test
    void shouldRejectAmbiguousSurnameOnlyMatches() {
        JSONArray playerDb = new JSONArray();

        JSONObject p1 = new JSONObject();
        p1.put("id", "1");
        p1.put("name", "Max Müller");
        JSONObject d1 = new JSONObject();
        d1.put("verein", "99");
        d1.put("possibleNames", new JSONArray(List.of("Max Mueller")));
        p1.put("data", d1);

        JSONObject p2 = new JSONObject();
        p2.put("id", "2");
        p2.put("name", "Thomas Müller");
        JSONObject d2 = new JSONObject();
        d2.put("verein", "99");
        d2.put("possibleNames", new JSONArray(List.of("Thomas Mueller")));
        p2.put("data", d2);

        playerDb.put(p1);
        playerDb.put(p2);

        Map<String, ClubObject> clubDb = new HashMap<>();
        ClubObject club = new ClubObject();
        club.setId("99");
        club.setName("FC Schalke 04");
        clubDb.put("99", club);

        assertNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "FC Schalke 04", Set.of("Müller")));
    }

    @Test
    void shouldMatchNameWithAccentAndPunctuationVariations() {
        JSONArray playerDb = new JSONArray();
        JSONObject player = new JSONObject();
        player.put("id", "7");
        player.put("name", "Berkay Yılmaz");
        JSONObject data = new JSONObject();
        data.put("verein", "88");
        data.put("possibleNames", new JSONArray(List.of("Berkay Yilmaz")));
        player.put("data", data);
        playerDb.put(player);

        Map<String, ClubObject> clubDb = new HashMap<>();
        ClubObject club = new ClubObject();
        club.setId("88");
        club.setName("Sport-Club Freiburg");
        clubDb.put("88", club);

        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "Sport-Club Freiburg", Set.of("Berkay Yılmaz")));
        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "Sport-Club Freiburg", Set.of("Berkay Yilmaz")));
    }

    @Test
    void shouldMatchNameWithNamePartsAndHyphenNormalization() {
        JSONArray playerDb = new JSONArray();
        JSONObject player = new JSONObject();
        player.put("id", "9");
        player.put("name", "Guéla Doué");
        JSONObject data = new JSONObject();
        data.put("verein", "11");
        data.put("possibleNames", new JSONArray(List.of("Gue la Doue", "Guela Doue")));
        player.put("data", data);
        playerDb.put(player);

        Map<String, ClubObject> clubDb = new HashMap<>();
        ClubObject club = new ClubObject();
        club.setId("11");
        club.setName("Bayer 04 Leverkusen");
        clubDb.put("11", club);

        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "Bayer 04 Leverkusen", Set.of("Guéla Doué")));
        assertNotNull(PlayerMatcher.findPlayerByNameAndClub(playerDb, clubDb, "Bayer 04 Leverkusen", Set.of("Guela Doue")));
    }
}