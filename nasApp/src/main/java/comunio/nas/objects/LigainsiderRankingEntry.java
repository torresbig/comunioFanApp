package comunio.nas.objects;

import org.json.JSONObject;

import comunio.nas.objects.orga.ComunioDate;

public class LigainsiderRankingEntry {
    public String name;
    public int rang;
    public String verein;
    public double durchschnittsnote;
    public double durchschnittspunkte;
    public int punkte;
    public int einsaetzeBewertet;
    public int durchschnittsminuten;
    public ComunioDate lastUpdate; 
    
    public LigainsiderRankingEntry(String name, int rang, String verein, double durchschnittsnote, double durchschnittspunkte, int punkte, int einsaetzeBewertet, int durchschnittsminuten) {
        this.name = name;
        this.rang = rang;
        this.verein = verein;
        this.durchschnittsnote = durchschnittsnote;
        this.durchschnittspunkte = durchschnittspunkte;
        this.punkte = punkte;
        this.einsaetzeBewertet = einsaetzeBewertet;
        this.durchschnittsminuten = durchschnittsminuten;
        this.lastUpdate = new ComunioDate();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("rang", rang);
        obj.put("verein", verein);
        obj.put("durchschnittsnote", durchschnittsnote);
        obj.put("durchschnittspunkte", durchschnittspunkte);
        obj.put("punkte", punkte);
        obj.put("einsaetzeBewertet", einsaetzeBewertet);
        obj.put("durchschnittsminuten", durchschnittsminuten);
        obj.put("lastUpdate", lastUpdate == null ? new ComunioDate() : lastUpdate.toString());
        return obj;
    }
    
}
