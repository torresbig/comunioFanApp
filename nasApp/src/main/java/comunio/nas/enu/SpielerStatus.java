package comunio.nas.enu;

public enum SpielerStatus {
	
	VERLETZT,
	AUFBAUTRAINING,
	GESPERRT,
	AKTIV,
	NICHT_IM_KADER,
	ROTE_KARTE,
	GELBROTE_KARTE,
	FUENFTE_GELBE_KARTE,
	SONSTIGES,
	NICHT_IN_LIGA,
	UNBESTIMMT;
	
	public static SpielerStatus fromString(String status) {
		SpielerStatus spStat = null;
		String upperStatus = status.toUpperCase();
		
		if (upperStatus.contains("AUFBAUTRAINING") || upperStatus.equals("REHABILITATION")) {
			spStat = SpielerStatus.AUFBAUTRAINING;
		} else if (upperStatus.contains("NICHT IM KADER") || upperStatus.equals("SUSPENDED") || upperStatus.equals("NICHT_IM_KADER")) {
			spStat = SpielerStatus.NICHT_IM_KADER;
		} else if (upperStatus.contains("ABWESEND")) {
			spStat = SpielerStatus.NICHT_IM_KADER;
		} else if (upperStatus.contains("NICHT IN LIGA") || upperStatus.equals("NICHT_IN_LIGA")) {
			spStat = SpielerStatus.NICHT_IN_LIGA;
		} else if (upperStatus.contains("GESPERRT") || upperStatus.contains("SPERRE") || upperStatus.contains("ROTE KARTE") || upperStatus.contains("RED_BANNED") || upperStatus.contains("ROTE_KARTE") || upperStatus.contains("GELB-ROTE KARTE") || upperStatus.contains("YELLOW_RED_BANNED") || upperStatus.contains("GELBROTE_KARTE") || upperStatus.contains("5. GELBE KARTE") || upperStatus.contains("YELLOW_BANNED") || upperStatus.contains("FUENFTE_GELBE_KARTE")) {
			spStat = SpielerStatus.GESPERRT;
		} else if (upperStatus.contains("VERLETZUNG") || upperStatus.equals("INJURED") || upperStatus.equals("VERLETZT") || upperStatus.contains("ANGESCHLAGEN") || upperStatus.equals("WEAKENED")) {
			spStat = SpielerStatus.VERLETZT;
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
	
	public boolean isGESPERRT() {
		return this == GESPERRT;
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
