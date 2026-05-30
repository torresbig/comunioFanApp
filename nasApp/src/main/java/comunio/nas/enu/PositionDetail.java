package comunio.nas.enu;

public enum PositionDetail {

	TW("Torwart"), 
	LIB("Libero"), 
	IV("Innenverteidiger"), 
	LV("Linker Verteidiger"), 
	RV("Rechter Verteidiger"), 
	DM("Defensives Mittelfeld"), 
	ZM("Zentrales Mittelfeld"), 
	RM("Rechtes Mittelfeld"), 
	LM("Linkes Mittelfeld"), 
	OM("Offensives Mittelfeld"), 
	LA("Linksaußen"), 
	RA("Rechtsaußen"), 
	HS("Hängende Spitze"), 
	MS("Mittelstürmer");

	String name;

	private PositionDetail(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	// Boolean-Methoden für jede Position
	public boolean isTW() {
		return this == TW;
	}

	public boolean isLIB() {
		return this == LIB;
	}

	public boolean isIV() {
		return this == IV;
	}

	public boolean isLV() {
		return this == LV;
	}

	public boolean isRV() {
		return this == RV;
	}

	public boolean isDM() {
		return this == DM;
	}

	public boolean isZM() {
		return this == ZM;
	}

	public boolean isRM() {
		return this == RM;
	}

	public boolean isLM() {
		return this == LM;
	}

	public boolean isOM() {
		return this == OM;
	}

	public boolean isLA() {
		return this == LA;
	}

	public boolean isRA() {
		return this == RA;
	}

	public boolean isHS() {
		return this == HS;
	}

	public boolean isMS() {
		return this == MS;
	}

	public static PositionDetail fromName(String name) {
		if (name == null) {
	        throw new IllegalArgumentException("Der Name darf nicht null sein.");
	    }
		for (PositionDetail position : PositionDetail.values()) {
			if (position.getName().equalsIgnoreCase(name)) {
				return position;
			} else if(position.toString().equalsIgnoreCase(name)) {
				return position;
			}
		}
		throw new IllegalArgumentException("Keine Position mit dem Namen " + name + " gefunden.");
	}

}
