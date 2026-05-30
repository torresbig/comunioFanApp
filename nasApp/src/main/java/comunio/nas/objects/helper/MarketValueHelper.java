package comunio.nas.objects.helper;

public class MarketValueHelper {

	/**
	 * Wandelt einen Marktwert-String wie "12,00 Mio. €" oder "150 Tsd. €" in einen
	 * int (Euro) um. Gibt 0 zurück, wenn nicht interpretierbar.
	 */
	public static int parseMarketValue(String value) {
		try {
			value = value.replace(".", "").replace("€", "").trim();
			if (value.endsWith("Mio")) {
				value = value.replace("Mio", "").replace(",", ".").trim();
				return (int) (Double.parseDouble(value) * 1_000_000);
			} else if (value.endsWith("Tsd")) {
				value = value.replace("Tsd", "").replace(",", ".").trim();
				return (int) (Double.parseDouble(value) * 1_000);
			} else {
				value = value.replace(",", "").replace(" ", "");
				return Integer.parseInt(value);
			}
		} catch (Exception e) {
			return 0;
		}
	}
	
}
