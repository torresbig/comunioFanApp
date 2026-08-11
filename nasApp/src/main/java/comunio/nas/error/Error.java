package comunio.nas.error;

import java.time.Instant;

import org.json.JSONObject;

public class Error {

	private Instant timestamp;
	private ErrorType errorType;
	private String message;
	private String methode;
	private String url;
	private String acception;

	public Error() {
		super();
		this.errorType = ErrorType.NONE;
	}

	public Error(ErrorType errorType, String message, String methode, String url, String acception) {
		super();
		this.setTimestamp(Instant.now());
		this.setErrorType(errorType);
		this.setMessage(message);
		this.setMethode(methode);
		this.setUrl(url);
		this.setAcception(acception);
	}
	
	public Error(ErrorType errorType, String message) {
		super();
		this.setTimestamp(Instant.now());
		this.setErrorType(errorType);
		this.setMessage(message);
		
	}


	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public void setErrorType(ErrorType errorType) {
		this.errorType = errorType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMethode() {
		return methode;
	}

	public void setMethode(String methode) {
		this.methode = methode;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAcception() {
		return acception;
	}

	public void setAcception(String acception) {
		this.acception = acception;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if(this.timestamp != null) {
			json.put("timestamp", this.timestamp.toString());
		}
		if(this.errorType != null) {
			json.put("errorType", this.errorType.toString());
		}
		if(this.message != null) {
			json.put("message", this.message);
		}
		if(this.methode != null) {
			json.put("methode", this.methode);
		}
		if(this.url != null) {
			json.put("url", this.url);
		}
		if(this.acception != null) {
			json.put("acception", this.acception);
		}
		return json;
	}

	public void fromJSON(JSONObject json) {
		if (json.has("timestamp")) {
			this.timestamp = Instant.parse(json.getString("timestamp"));

		} else {
			this.timestamp = Instant.EPOCH;
		}
		if (json.has("errorType")) {
			this.errorType = ErrorType.valueOf(json.getString("errorType"));
		} else {
			this.errorType = ErrorType.NONE;
		}
		if (json.has("message")) {
			this.message = json.getString("message");
		} else {
			this.message = "";
		}
		if (json.has("methode")) {
			this.methode = json.getString("methode");
		} else {
			this.methode = "";
		}
		if (json.has("url")) {
			this.url = json.getString("url");
		} else {
			this.url = "";
		}
		if (json.has("acception")) {
			this.acception = json.getString("acception");
		} else {
			this.acception = "";
		}

	}
	
	// Error.java  (nur die relevanten Zeilen)
	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof Error)) return false;
	    Error other = (Error) o;
	    return compare(other);          // compare definiert exakt dieselben Kriterien
	}

	
	public boolean compare(Error other) {
		if (other == null) {
			return false;
		}
		return this.errorType == other.errorType &&
				this.message.equals(other.message) &&
				this.methode.equals(other.methode) &&
				this.url.equals(other.url) &&
				this.acception.equals(other.acception);
	}

}
