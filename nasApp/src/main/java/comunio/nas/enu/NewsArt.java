package comunio.nas.enu;

public enum NewsArt {
	
	TRANSFER, //pk
	OWNERCHANGE,
	VEREINSWECHSEL, // ok
	PUNKTEKORREKTUR, // ok
	BONUSZAHLUNG, // ok
	STRAFZAHLUNG, // ok
	NAMENSAENDERUNG,// ok
	SPIELERSTATUS, // ok
	SPIELERSTATUSCOMUNIO, // ok
	POSITIONSWECHSEL, // ok
	USERPOINTS,
	ELFDESTAGES,
	AUFSTELLUNG,
	ANGEBOT, 
	KEINE_NEWS,
	NEW_PLAYER,
	MOVE_FROM_NOTINLIGA_PLAYER,
	SAISONUEBERGANG,
	UNBESTIMMT;
	
	public boolean isTRANSFER() {
		return this == TRANSFER;
	}
	
	public boolean isOWNERCHANGE(){
		return this == OWNERCHANGE;
	}
	
	public boolean isVEREINSWECHSEL() {
		return this == VEREINSWECHSEL;
	}
	
	public boolean isPUNKTEKORREKTUR() {
		return this == PUNKTEKORREKTUR;
	}
	
	public boolean isBONUSZAHLUNG() {
		return this == BONUSZAHLUNG;
	}
	
	public boolean isAUFSTELLUNG() {
		return this == AUFSTELLUNG;
	}
	
	public boolean isUSERPOINTS() {
		return this == USERPOINTS;
	}
	
	public boolean isSPIELERSTATUS() {
		return this == SPIELERSTATUS;
	}
	
	public boolean isSPIELERSTATUSCOMUNIO() {
		return this == SPIELERSTATUSCOMUNIO;
	}
	
	public boolean isNAMENSAENDERUNG() {
		return this == NAMENSAENDERUNG;
	}
	
	public boolean isSTRAFZAHLUNG() {
		return this == STRAFZAHLUNG;
	}
	
	public boolean isPOSITIONSWECHSEL() {
		return this == POSITIONSWECHSEL;
	}
	
	public boolean isANGEBOT() {
		return this == ANGEBOT;
	}
	
	public boolean isKEINE_NEWS(){
		return this == KEINE_NEWS;
	}
	
	public boolean isNEW_PLAYER(){
		return this == NEW_PLAYER;
	}
	
	public boolean isMOVE_FROM_NOTINLIGA_PLAYER(){
		return this == MOVE_FROM_NOTINLIGA_PLAYER;
	}
	
	public boolean isUNBESTIMMT() {
		return this == UNBESTIMMT;
	}

	public boolean isSAISONUEBERGANG() {
		return this == SAISONUEBERGANG;
	}
	public boolean isELFDESTAGES() {
		return this == ELFDESTAGES;
	}
}
