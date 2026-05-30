package comunio.nas.objects.helper;

import java.io.File;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NachholspielFixer {


    private static final Set<String> NACHHOL_VEREIN_IDS = Set.of("8", "4");

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        File file19 = new File("C:\\Users\\thoma\\Desktop\\24te.txt");
        Map<String,Object> data19 = mapper.readValue(file19, new TypeReference<Map<String,Object>>() {});

        List<Map<String,Object>> players19 = (List<Map<String,Object>>) data19.get("playerDB");

        int nachholProcessed = 0;

        // === SCHRITT 1: NACHHOLSpieler - value(19) → value(16) kopieren ===
        for (Map<String,Object> p19 : players19) {
            Map<String,Object> d19 = (Map<String,Object>) p19.get("data");
            if (d19 == null) continue;

            String vereinId = String.valueOf(d19.getOrDefault("verein", ""));
            if (!NACHHOL_VEREIN_IDS.contains(vereinId)) continue;

            List<Map<String,Object>> s19 = (List<Map<String,Object>>) 
                d19.getOrDefault("spieltagspunkte", new ArrayList<>());
            Map<Integer, Map<String,Object>> byKey19 = indexByKey(s19);

            // 1. EXISTIERENDEN value(19) holen (hart!)
            Map<String,Object> e19 = byKey19.get(24);
            Map<String,Object> e16 = byKey19.get(17);
            
            if (e19 == null || e19.get("value") == null) {
                continue; // kein value(19) vorhanden
            }

            int value19Vorhanden = toInt(e19.get("value"));

            // 2. Spieltag 16 erstellen/finden
            if (e16 == null) {
                e16 = new LinkedHashMap<>();
                e16.put("key", 17);
                e16.put("totalPoints", 0);
                e16.put("value", 0);
                s19.add(e16);
                byKey19.put(17, e16);
            }

            // 3. value(19) 1:1 auf value(16) KOPIEREN
            e16.put("value", value19Vorhanden);
            e16.put("nachholspiel", true);

            // 4. value(19) = 0 setzen
            e19.put("value", 0);

            // 5. totalPoints 16-18 hochrechnen
            Map<String,Object> e15 = byKey19.get(16);
            int currentTotal = e15 != null ? toInt(e15.get("totalPoints")) : 0;

            List<Integer> keys16to18 = new ArrayList<>();
            for (int k = 17; k <= 23; k++) {
                if (byKey19.containsKey(k)) {
                    keys16to18.add(k);
                }
            }
            Collections.sort(keys16to18);

            for (int k : keys16to18) {
                Map<String,Object> e = byKey19.get(k);
                int val = toInt(e.get("value"));
                currentTotal += val;
                e.put("totalPoints", currentTotal);
            }

            d19.put("spieltagspunkte", s19);
            nachholProcessed++;
        }

        System.out.println("✓ " + nachholProcessed + " Nachholspieler korrigiert");

        // === SCHRITT 2: FÜR ALLE value(19) = total19 - total18 ===
        int allProcessed = 0;
        for (Map<String,Object> p19 : players19) {
            Map<String,Object> d19 = (Map<String,Object>) p19.get("data");
            if (d19 == null) continue;

            List<Map<String,Object>> s19 = (List<Map<String,Object>>) 
                d19.getOrDefault("spieltagspunkte", new ArrayList<>());
            Map<Integer, Map<String,Object>> byKey19 = indexByKey(s19);

            Map<String,Object> e19 = byKey19.get(24);
            Map<String,Object> e18 = byKey19.get(23);
            
            Integer total19 = e19 != null ? toIntObj(e19.get("totalPoints")) : null;
            Integer total18 = e18 != null ? toIntObj(e18.get("totalPoints")) : null;

            if (e19 != null && total19 != null) {
                int value19Final = total18 != null ? total19 - total18 : total19;
                e19.put("value", value19Final);
            }

            d19.put("spieltagspunkte", s19);
            allProcessed++;
        }

        System.out.println("✓ " + allProcessed + " Alle Spieler value(24) korrigiert");
        
        File out = new File("C:\\Users\\thoma\\Desktop\\final.txt");
        mapper.writerWithDefaultPrettyPrinter().writeValue(out, data19);
        System.out.println("✓ final.txt geschrieben");
    }

    private static Integer toIntObj(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); }
        catch (Exception e) { return null; }
    }

    private static int toInt(Object o) {
        Integer i = toIntObj(o);
        return i != null ? i : 0;
    }

    private static Map<Integer, Map<String,Object>> indexByKey(List<Map<String,Object>> list) {
        Map<Integer, Map<String,Object>> map = new HashMap<>();
        for (Map<String,Object> e : list) {
            Object k = e.get("key");
            if (k instanceof Number) {
                map.put(((Number) k).intValue(), e);
            } else if (k != null) {
                try { map.put(Integer.parseInt(k.toString()), e); }
                catch (Exception ignored) {}
            }
        }
        return map;
    }
}