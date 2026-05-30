package comunio.nas.enu;

public enum SpielerStatus {
	
	VERLETZT ,
	AUFBAUTRAINING,
	AKTIV ,
	NICHT_IM_KADER ,
	ROTE_KARTE,
	GELBROTE_KARTE,
	FUENFTE_GELBE_KARTE ,
	SONSTIGES ,
	NICHT_IN_LIGA ,
	UNBESTIMMT ;
	
	
	public static SpielerStatus fromString(String status) {
		SpielerStatus spStat = null;
		if (status.toUpperCase().contains("Aufbautraining".toUpperCase()) || status.toUpperCase().equals("REHABILITATION".toUpperCase()) ) {
			spStat = SpielerStatus.AUFBAUTRAINING;
		} else if (status.toUpperCase().contains("Nicht im Kader".toUpperCase()) || status.toUpperCase().equals("SUSPENDED") || status.toUpperCase().equals("NICHT_IM_KADER".toUpperCase())) {
			spStat = SpielerStatus.NICHT_IM_KADER;
		} else if (status.toUpperCase().contains("Abwesend".toUpperCase())) {
			spStat = SpielerStatus.NICHT_IM_KADER;
		} else if (status.toUpperCase().contains("Nicht in Liga".toUpperCase()) || status.toUpperCase().equals("NICHT_IN_LIGA".toUpperCase())) {
			spStat = SpielerStatus.NICHT_IN_LIGA;
		} else if (status.toUpperCase().contains("Verletzung".toUpperCase()) || status.toUpperCase().equals("INJURED".toUpperCase()) || status.toUpperCase().equals("VERLETZT".toUpperCase())) {
			spStat = SpielerStatus.VERLETZT;
		} else if (status.toUpperCase().contains("Schwerer angeschlagen".toUpperCase())) {
			spStat = SpielerStatus.VERLETZT;
		} else if (status.toUpperCase().contains("Angeschlagen".toUpperCase()) || status.toUpperCase().equals("WEAKENED".toUpperCase())) {
			spStat = SpielerStatus.VERLETZT;
		} else if (status.toUpperCase().contains("Rote Karte".toUpperCase()) || status.toUpperCase().equals("RED_BANNED".toUpperCase())|| status.toUpperCase().equals("ROTE_KARTE".toUpperCase())) {
			spStat = SpielerStatus.ROTE_KARTE;
		} else if (status.toUpperCase().contains("Gelb-Rote Karte".toUpperCase()) || status.toUpperCase().equals("YELLOW_RED_BANNED".toUpperCase())|| status.toUpperCase().equals("GELBROTE_KARTE".toUpperCase())) {
			spStat = SpielerStatus.GELBROTE_KARTE;
		} else if (status.toUpperCase().contains("5. Gelbe Karte".toUpperCase()) || status.toUpperCase().equals("YELLOW_BANNED".toUpperCase())|| status.toUpperCase().equals("FUENFTE_GELBE_KARTE".toUpperCase())) {
			spStat = SpielerStatus.FUENFTE_GELBE_KARTE;
		} else if (status.isEmpty()) {
			spStat = SpielerStatus.UNBESTIMMT;
		} else {
			spStat = SpielerStatus.AKTIV;
		}
		return spStat;
	}
	
	public boolean isAKTIV() {
		return this == AKTIV;
	}
	
	public boolean isVERLETZT() {
		return this == VERLETZT;
	}
	
	public boolean isAUFBAUTRAINING() {
		return this == AUFBAUTRAINING;
	}
	
	public boolean isNICHT_IM_KADER() {
		return this == NICHT_IM_KADER;
	}
	
	public boolean isROTE_KARTE() {
		return this == ROTE_KARTE;
	}
	
	public boolean isGELBROTE_KARTE() {
		return this == GELBROTE_KARTE;
	}
	
	public boolean isFUENFTE_GELBE_KARTE() {
		return this == FUENFTE_GELBE_KARTE;
	}
	
	public boolean isSONSTIGES() {
		return this == SONSTIGES;
	}
	
	public boolean isNICHT_IN_LIGA() {
		return this == NICHT_IN_LIGA;
	}
	
	public boolean isUNBESTIMMT() {
		return this == UNBESTIMMT;
	}
	
}
