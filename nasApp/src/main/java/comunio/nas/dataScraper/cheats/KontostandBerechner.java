package comunio.nas.dataScraper.cheats;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.user.User;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Berechnet und verwaltet Kontostände von Comunio-Usern unter Berücksichtigung von Transfers und Punkten.
 */
public final class KontostandBerechner {
    private static final Logger LOGGER = Logger.getLogger(KontostandBerechner.class.getName());
    private static final int STARTGUTHABEN = 20000000;
    private static final int PUNKTE_MULTIPLIKATOR = 10000;


    /**
     * Berechnet die Kontostände aller User basierend auf Transfers und Punkten
     * Aktualisiert die User-Objekte in der übergebenen Map direkt
     * @param userMap Map mit userId als Schlüssel und User-Objekt als Wert
     * @param newsManager NewsManager für Transfer-News
     */
    public void calculateKontostaende(Map<String, User> userMap, NewsManager newsManager) {
        Objects.requireNonNull(userMap, "userMap darf nicht null sein");
        
        // Initialisiere die Kontostand-Felder für alle User
        for (User user : userMap.values()) {
            int initialGuthaben = "Computer".equalsIgnoreCase(user.getFirstName()) ? 0 : STARTGUTHABEN;
            user.setGuthaben(initialGuthaben);
            user.setSellValue(0);
            user.setBuyValue(0);
            user.setNegativesBudget(false); // Wird später basierend auf Guthaben gesetzt
        }
        
        processTransfers(newsManager, userMap);
        processPunkte(userMap);
    }

    private void processTransfers(NewsManager newsManager, Map<String, User> userMap) {
        if (newsManager == null) return;

        newsManager.getNewsByDate().values().stream()
            .flatMap(List::stream)
            .filter(news -> news != null && news.getArt().isTRANSFER())
            .forEach(news -> processTransfer(news, userMap));
    }

    /**
     * Verarbeitet einen Transfer und aktualisiert:
     * - Guthaben von Käufer/Verkäufer
     * - Summe aller Käufe/Verkäufe (buyValue/sellValue)
     * - negativesBudget Flag
     */
    private void processTransfer(News transferNews, Map<String, User> userMap) {
        try {
            String transferString = transferNews.getText();
            if (transferString == null || transferString.isBlank()) return;

            JSONObject transfer = new JSONObject(transferString);
            String sellerId = resolveUserId(transfer.getString("seller"), transfer.optString("sellerId"), userMap);
            String buyerId = resolveUserId(transfer.getString("buyer"), transfer.optString("buyerId"), userMap);
            int price = transfer.getInt("price");

            // Verkäufer bekommt Geld
            if (sellerId != null && userMap.containsKey(sellerId)) {
                User seller = userMap.get(sellerId);
                long alt = seller.getGuthaben();
                seller.setGuthaben(alt + price);
                int sell = seller.getSellValue();
                seller.setSellValue(sell + price);
                seller.setNegativesBudget((alt + price) < 0);
            }

            // Käufer zahlt
            if (buyerId != null && userMap.containsKey(buyerId)) {
                User buyer = userMap.get(buyerId);
                long alt = buyer.getGuthaben();
                buyer.setGuthaben(alt - price);
                int buy = buyer.getBuyValue();
                buyer.setBuyValue(buy + price);
                buyer.setNegativesBudget((alt - price) < 0);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei Transfer-Verarbeitung: " + e.getMessage(), e);
        }
    }

    private String resolveUserId(String name, String id, Map<String, User> userMap) {
        if (id != null && !id.isBlank() && !id.equals(name)) {
            return id;
        }
        for (User user : userMap.values()) {
            if (name.equalsIgnoreCase(user.getFirstName())) {
                return user.getId();
            }
        }
        LOGGER.warning("Unbekannter User: " + name);
        return null;
    }

    private void processPunkte(Map<String, User> userMap) {
        for (User user : userMap.values()) {
            Map<Integer, Integer> punkteHistorie = user.getPunkteHistorie();
            int sum = 0;
            int sumPunkte = 0;
            int countedSpieltage = 0;
            for (Map.Entry<Integer, Integer> entry : punkteHistorie.entrySet()) {
                int key = entry.getKey();
                int punkte = entry.getValue();
                System.out.printf("Spieltag %s: %d Punkte%n", key, punkte);
                sumPunkte += punkte;
                if (punkte > 0) {
                    sum += punkte * PUNKTE_MULTIPLIKATOR;
                    countedSpieltage++;
                }
            }

            long altesGuthaben = user.getGuthaben();
            user.setPunkteGeld(sum);
            user.setGuthaben(altesGuthaben + sum);
            user.setNegativesBudget((altesGuthaben + sum) < 0);
        }
    }

    public String toString() {
    	return "KontostandBerechner [STARTGUTHABEN=" + STARTGUTHABEN + ", PUNKTE_MULTIPLIKATOR=" + PUNKTE_MULTIPLIKATOR + "]";
    }
}