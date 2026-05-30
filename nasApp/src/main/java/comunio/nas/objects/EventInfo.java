package comunio.nas.objects;

import org.json.JSONObject;

import comunio.nas.objects.orga.ComunioDate;

public class EventInfo {
	
//	"eventInfo": {
//		"date": "2025-09-12T00:00:00+0200",
//		"next": 392944,
//		"kickoff": "2025-09-12T14:30:00+0200",
//		"previous": 392924,
//		"id": 392934,
//		"event": "Aufstellung 3. Spieltag"
//	}
	
	private ComunioDate date;
	private long next;
	private long previous;
	private long id;
	private String event; 
	
	public EventInfo() {
		
	}
	
	public EventInfo(ComunioDate date, long next, long previous, long id, String event) {
		this.setDate(date);
		this.setNext(next); 
		this.setPrevious(previous);
		this.setId(id);
		this.setEvent(event); 
	}
	
	public static EventInfo fromJson(JSONObject json) {
		if(json == null || json.isEmpty()) {
			return null;
		}
		EventInfo result = new EventInfo();
		if(json.has("date")) {
			result.setDate(json.getString("date"));
		}
		if(json.has("next")) {
			result.setNext(json.getLong("next"));
		}
		if(json.has("previous")) {
			result.setPrevious(json.getLong("previous"));
		}
		if(json.has("id")) {
			result.setId(json.getLong("id"));
		}
		if(json.has("event")) {
			result.setEvent(json.getString("event"));
		}
		
		return result;
	}
	
	public JSONObject toJson() {
		JSONObject result = new JSONObject();
		result.put("date", this.getDate().toString());
		result.put("next", this.getNext());
		result.put("previous", this.getPrevious());
		result.put("id", this.getId());
		result.put("event", this.getEvent());
		return result; 
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getPrevious() {
		return previous;
	}

	public void setPrevious(long previous) {
		this.previous = previous;
	}

	public long getNext() {
		return next;
	}

	public void setNext(long next) {
		this.next = next;
	}

	public ComunioDate getDate() {
		return date;
	}

	public void setDate(ComunioDate date) {
		this.date = date;
	}
	
	public void setDate(String date) {
		this.date = new ComunioDate(date);
	}

}
