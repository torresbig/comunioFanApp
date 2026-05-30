package comunio.nas.enu;

public enum Position {

	TORHÜTER, ABWEHR, MITTELFELD, STURM, UNBESTIMMT;

	public static Position fromString(String name) {
		switch (name.toLowerCase()) {
		case "keeper":
		case "torward":
		case "torhüter":
		case "tormann":
			return TORHÜTER;
		case "defender":
		case "abwehr":
		case "verteidigung":
		case "verteidiger":
			return ABWEHR;
		case "striker":
		case "sturm":
		case "stürmer":
			return STURM;
		case "midfielder":
		case "mittelfeld":
			return MITTELFELD;
		default:
			return UNBESTIMMT;
		}
	}
	public boolean isTORHÜTER() {
		return this == TORHÜTER;
	}
	
	public boolean isABWEHR() {
		return this == ABWEHR;
	}
	
	public boolean isMITTELFELD() {
		return this == MITTELFELD;
	}
	
	public boolean isSTURM() {
		return this == STURM;
	}
	
	public boolean isUNBESTIMMT() {
		return this == UNBESTIMMT;
	}
}
