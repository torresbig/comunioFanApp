package comunio.nas.dataScraper.cheats;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Berechnet und verwaltet Kontostände von Comunio-Usern unter Berücksichtigung von Transfers und Punkten.
 */
public final class KontostandBerechner {
    private static final Logger LOGGER = Logger.getLogger(KontostandBerechner.class.getName());
    private static final int STARTGUTHABEN = 20_000_000;
    private static final int PUNKTE_MULTIPLIKATOR = 10_000;



    /**
     * Berechnet die Kontostände aller User basierend auf Transfers und Punkten
     * @param userArray JSON-Array mit User-Daten
     * @param newsManager NewsManager für Transfer-News
     * @return Aktualisiertes JSON-Array mit Kontoständen
     */
    public JSONArray calculateKontostaende(JSONArray userArray, NewsManager newsManager) {
        Objects.requireNonNull(userArray, "userArray darf nicht null sein");
        
        Map<String, JSONObject> userContainerMap = userArrayToMap(userArray);
        processTransfers(newsManager, userContainerMap);
        processPunkte(userContainerMap);
        
        return toJsonArray(userContainerMap);
    }

    private Map<String, JSONObject> userArrayToMap(JSONArray userArray) {
        Map<String, JSONObject> userContainerMap = new HashMap<>();
        for (int i = 0; i < userArray.length(); i++) {
            try {
                JSONObject userContainer = userArray.getJSONObject(i);
                JSONObject user = userContainer.getJSONObject("user");

                String userId = user.getString("id");
                int initialGuthaben = "Computer".equalsIgnoreCase(user.optString("firstName")) ? 0 : STARTGUTHABEN;
                userContainer.put("guthaben", initialGuthaben);
                userContainer.put("sellValue", 0);
                userContainer.put("buyValue", 0);
                
                userContainerMap.put(userId, userContainer);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler bei User-Verarbeitung: " + e.getMessage(), e);
            }
        }
        return userContainerMap;
    }

    private void processTransfers(NewsManager newsManager, Map<String, JSONObject> userContainerMap) {
        if (newsManager == null) return;

        newsManager.getNewsByDate().values().stream()
            .flatMap(List::stream)
            .filter(news -> news != null && news.getArt().isTRANSFER())
            .forEach(news -> processTransfer(news, userContainerMap));
    }

    /**
     * Verarbeitet einen Transfer und aktualisiert:
     * - Guthaben von Käufer/Verkäufer
     * - Summe aller Käufe/Verkäufe (buyValue/sellValue)
     * - negativesBudget Flag
     */
    private void processTransfer(News transferNews, Map<String, JSONObject> userContainerMap) {
        try {
            String transferString = transferNews.getText();
            if (transferString == null || transferString.isBlank()) return;

            JSONObject transfer = new JSONObject(transferString);
            String sellerId = resolveUserId(transfer.getString("seller"), transfer.optString("sellerId"), userContainerMap);
            String buyerId = resolveUserId(transfer.getString("buyer"), transfer.optString("buyerId"), userContainerMap);
            int price = transfer.getInt("price");

            // Verkäufer bekommt Geld
            if (sellerId != null && userContainerMap.containsKey(sellerId)) {
                JSONObject sellerContainer = userContainerMap.get(sellerId);
                int alt = sellerContainer.getInt("guthaben");
                sellerContainer.put("guthaben", alt + price);
                int sell = sellerContainer.optInt("sellValue",0);
                sellerContainer.put("sellValue", sell + price);
                JSONObject user = sellerContainer.optJSONObject("user");
                if (user != null) {
                    user.put("negativesBudget", (alt + price) < 0);
                }
            }

            // Käufer zahlt
            if (buyerId != null && userContainerMap.containsKey(buyerId)) {
                JSONObject buyerContainer = userContainerMap.get(buyerId);
                int alt = buyerContainer.getInt("guthaben");
                buyerContainer.put("guthaben", alt - price);
                int buy = buyerContainer.optInt("buyValue",0);
                buyerContainer.put("buyValue", buy + price);
                JSONObject user = buyerContainer.optJSONObject("user");
                if (user != null) {
                    user.put("negativesBudget", (alt - price) < 0);
                }

            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei Transfer-Verarbeitung: " + e.getMessage(), e);
        }
    }

    private String resolveUserId(String name, String id, Map<String, JSONObject> userContainerMap) {
        if (id != null && !id.isBlank() && !id.equals(name)) {
            return id;
        }
        for (JSONObject container : userContainerMap.values()) {
            JSONObject user = container.getJSONObject("user");
            if (name.equalsIgnoreCase(user.optString("firstName"))) {
                return user.getString("id");
            }
        }
        LOGGER.warning("Unbekannter User: " + name);
        return null;
    }

    private void processPunkte(Map<String, JSONObject> userContainerMap) {
        for (JSONObject userContainer : userContainerMap.values()) {
            if (!"5981249".equals(userContainer.optJSONObject("user").optString("id"))) {
                continue; // Nur für Thomas debuggen
            }
            JSONObject punkteHistorie = userContainer.optJSONObject("punkteHistorie", new JSONObject());
            int sum = 0;
            int sumPunkte = 0;
            int countedSpieltage = 0;
            Iterator<String> keys = punkteHistorie.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int punkte = punkteHistorie.optInt(key, -999); // -999 als Marker für Fehler
                System.out.printf("Spieltag %s: %d Punkte%n", key, punkte);
                sumPunkte += punkte;
                if (punkte > 0) {
                    sum += punkte * PUNKTE_MULTIPLIKATOR;
                    countedSpieltage++;
                }
            }

            System.out.println("Gezählte Spieltage: " + countedSpieltage);
            System.out.println("Berechnete Summe: " + sum);
            System.out.println("Gesamte Punkte: " + sumPunkte);
            System.out.println("Erwartete Summe: " + (1404 * PUNKTE_MULTIPLIKATOR));

            
            int altesGuthaben = userContainer.getInt("guthaben");
            userContainer.put("punktegeld", sum);
            userContainer.put("guthaben", altesGuthaben + sum);
            userContainer.getJSONObject("user").put("negativesBudget", (altesGuthaben + sum) < 0);
        }
    }

    private JSONArray toJsonArray(Map<String, JSONObject> userContainerMap) {
        JSONArray result = new JSONArray();
        userContainerMap.values().forEach(result::put);
        return result;
    }
}
