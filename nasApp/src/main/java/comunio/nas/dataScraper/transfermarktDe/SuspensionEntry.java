package comunio.nas.dataScraper.transfermarktDe;

import java.time.LocalDate;

public class SuspensionEntry {

	public String saison;
	public String art; // Ausfall/Sperre
	public String wettbewerb; // Titel aus dem <img>
	public LocalDate von;
	public LocalDate bis; // kann null sein
	public long tage;
	public int verpassteSpiele;

	public SuspensionEntry(String saison, String art, String wettbewerb, LocalDate von, LocalDate bis, long tage, int verpassteSpiele) {
		this.saison = saison;
		this.art = art;
		this.wettbewerb = wettbewerb;
		this.von = von;
		this.bis = bis;
		this.tage = tage;
		this.verpassteSpiele = verpassteSpiele;
	}
}
