package it.polito.verefoo.astrid.yaml;

public class Policy {
	private String from;
	private String to;
	private String action;

	public String getAction() {
		return action;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public void setTo(String to) {
		this.to = to;
	}
}

