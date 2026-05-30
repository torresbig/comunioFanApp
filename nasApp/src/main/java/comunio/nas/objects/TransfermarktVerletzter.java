package comunio.nas.objects;

import java.util.Set;

public class TransfermarktVerletzter {
    public String name;
    public String id;
    public String verletzung;
    public String seit;
    public String bis;
    public String link;
    public String nation;
    public String marktwert;
    public String tmVereinName;
    public Set<String> nameVariants;

    public TransfermarktVerletzter(String name, String id, String verletzung, String seit, String bis, String link,
                                   String nation, String marktwert, String tmVereinName, Set<String> nameVariants) {
        this.name = name;
        this.id = id;
        this.verletzung = verletzung;
        this.seit = seit;
        this.bis = bis;
        this.link = link;
        this.nation = nation;
        this.marktwert = marktwert;
        this.tmVereinName = tmVereinName;
        this.nameVariants = nameVariants;
    }
}

