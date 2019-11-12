package it.polito.verefoo.rest.spring;

import java.util.HashMap;

import it.polito.verefoo.astrid.jaxb.InfrastructureInfo;
import it.polito.verefoo.astrid.yaml.PolicyGraph;
import it.polito.verefoo.jaxb.NFV;

public class Database {
	private static volatile Database PolicyDB_instance = null; 
	private HashMap<String,PolicyGraph> mapPolicies= null;
	private HashMap<String,InfrastructureInfo> mapInfrastructureInfo= null;
	
	private Database(){
		mapPolicies = new HashMap<String, PolicyGraph>();
		mapInfrastructureInfo = new HashMap<String, InfrastructureInfo>();
	}
	
	public static Database getInstance()  
    { 
        if (PolicyDB_instance == null) {
        	synchronized (Database.class) {   //Check for the second time.
                //if there is no instance available... create new one
                if (PolicyDB_instance == null) {
                	PolicyDB_instance = new Database();
                }
        	}
        }
        return PolicyDB_instance; 
    }

	public void addPolicy(PolicyGraph policy) {
		mapPolicies.put(policy.getMetadata().getName(), policy);
	}
	
	

	public boolean existsPolicy(String name) {
		return mapPolicies.containsKey(name);
	}

	public PolicyGraph getPolicy(String name) {
		return mapPolicies.get(name);
	}

	public void addInfo(InfrastructureInfo info) {
		mapInfrastructureInfo.put(info.getMetadata().getName(), info);
	}

	public InfrastructureInfo getInfo(String graphName) {
		return mapInfrastructureInfo.get(graphName);
	} 
	

}
