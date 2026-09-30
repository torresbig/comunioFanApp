package comunio.nas.objects.ligainsider;

import org.json.JSONObject;

import comunio.nas.objects.orga.ComunioDate;

public class LigainsiderRankingEntry {
    public String name;
    public String id; 
    public String link; 
    public int rang;
    public String verein;
    public double durchschnittsnote;
    public double durchschnittspunkte;
    public int punkte;
    public int einsaetzeBewertet;
    public int durchschnittsminuten;
    public ComunioDate lastUpdate; 
    
    public LigainsiderRankingEntry(String name, String id, String link, int rang, String verein, double durchschnittsnote, double durchschnittspunkte, int punkte, int einsaetzeBewertet, int durchschnittsminuten) {
        this.name = name;
        this.id = id; 
        this.link = link;
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
    
    /*
     * die methode lierfert nur die daten wie name id und link. alles andere wird in attribute geschreiben und über die methode toJSON
     */
    public JSONObject getJSONforPlayerdata() {
    	JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("id", id);
        obj.put("link", link);
        return obj;
    }
    
}
