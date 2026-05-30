package comunio.nas.util;

public class ZahlenParser {
	
	public static int parseJsonValueToInt(Object value) {
	    if (value == null) {
	        return 0;
	    }
	    if (value instanceof Number) {
	        return ((Number) value).intValue();
	    }
	    String valStr = value.toString().trim();
	    if (valStr.equalsIgnoreCase("n/a") || valStr.equals("-") || valStr.isEmpty()) {
	        return 0;
	    }
	    try {
	        return Integer.parseInt(valStr);
	    } catch (NumberFormatException ex) {
	        return 0;
	    }
	}


}
