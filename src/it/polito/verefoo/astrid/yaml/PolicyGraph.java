package it.polito.verefoo.astrid.yaml;

import java.util.List;
import java.util.Map;

public class PolicyGraph {
	
	private String kind;
	private Metadata metadata;
	private String lastName;

	

	private Specification spec;
	public String getKind() {
		return kind;
	}

	public String getLastName() {
		return lastName;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Specification getSpec() {
		return spec;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	 public void setSpec(Specification spec) {
		this.spec = spec;
	}
		

}


