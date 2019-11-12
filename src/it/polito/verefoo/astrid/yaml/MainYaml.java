package it.polito.verefoo.astrid.yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import com.google.common.base.Charsets;

import it.polito.verefoo.VerefooSerializer;
import it.polito.verefoo.astrid.jaxb.*;
import it.polito.verefoo.astrid.jaxb.InfrastructureInfo.Spec.Service;
import it.polito.verefoo.astrid.jaxb.InfrastructureInfo.Spec.Service.Instance;
import it.polito.verefoo.jaxb.*;
import it.polito.verefoo.rest.spring.Database;
import it.polito.verigraph.extra.Tuple;

public class MainYaml {
	
	public static void main(String[] args) throws IOException {
		 Database db= Database.getInstance();
		 PolicyGraph val = parsePolicies(convertFileToString("./testfile/Astrid/complex_policy.yml"));
		 db.addPolicy(val);
		 NFV nfv = parseGraph(partialGraph("./testfile/Astrid/infrastructure-info.xml"), val);
		//nfv = parseGraph(partialGraph("./testfile/Astrid/infrastructure-info.xml"), null);
		retrieveConfiguration(nfv);
	}

	
	  private static NFV retrieveConfiguration(NFV nfv) throws MalformedURLException {
	  System.setProperty("log4j.configuration", new File("resources",  "log4j2.xml").toURI().toURL().toString()); 
	  Logger loggerResult =	  LogManager.getLogger("result");
	  
	  JAXBContext jc = null; VerefooSerializer test = null; 
	  // create a JAXBContext	  capable of handling the generated classes 
	  try { jc =
	  JAXBContext.newInstance("it.polito.verefoo.jaxb"); Marshaller m =
	  jc.createMarshaller(); m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
	  Boolean.TRUE); m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
	  "./xsd/nfvSchema.xsd");
	  
	  StringWriter stringWriter2 = new StringWriter(); m.marshal(nfv,  stringWriter2); System.out.println(stringWriter2.toString());
	  
	  
	  test = new VerefooSerializer(nfv); if (test.isSat()) {
	  removeFilters(test.getResult()); loggerResult.info("SAT");
	  loggerResult.info("----------------------OUTPUT----------------------");
	  StringWriter stringWriter = new StringWriter(); m.marshal(test.getResult(),
	  System.out); // for debug purpose m.marshal(test.getResult(), stringWriter);
	  loggerResult.info(stringWriter.toString());
	  loggerResult.info("--------------------------------------------------"); }
	  else { loggerResult.info("UNSAT");
	  loggerResult.info("----------------------OUTPUT----------------------");
	  StringWriter stringWriter = new StringWriter(); m.marshal(test.getResult(),
	  stringWriter); loggerResult.info(stringWriter.toString());
	  loggerResult.info("--------------------------------------------------");
	  System.exit(1); }
	  
	  } catch (JAXBException e) { e.printStackTrace(); } return test.getResult(); }
	 

	public static NFV parseGraph(InfrastructureInfo inf, PolicyGraph policyGraph) {
		//System.out.println("+++ IN metadata:" + inf.getMetadata().getName());
		NFV nfv = new NFV();
		Graphs graphs = new Graphs();
		PropertyDefinition pd = new PropertyDefinition();
		Constraints cnst = new Constraints();
		NodeConstraints nc = new NodeConstraints();
		LinkConstraints lc = new LinkConstraints();

		cnst.setNodeConstraints(nc);
		cnst.setLinkConstraints(lc);
		nfv.setGraphs(graphs);
		nfv.setPropertyDefinition(pd);
		nfv.setConstraints(cnst);
		Graph graph = new Graph();
		graph.setId(Math.abs((long) inf.getMetadata().getName().hashCode()) % 100);

		HashMap<String, Neighbour> fwNObjectMap = new HashMap<String, Neighbour>();
		HashMap<Node, ArrayList<Neighbour>> nodeNeighborFilterMap = new HashMap<>();

		for (Service services : inf.getSpec().getService()) {
			for (Instance instance : services.getInstance()) {
				// WEBSERVER
				Node server = new Node();
				server.setFunctionalType(FunctionalTypes.WEBSERVER);
				server.setName(instance.getIp());
				Configuration confS = new Configuration();
				confS.setName(services.getName());
				confS.setDescription(services.getPort().getInternal()+"");
				Webserver ws = new Webserver();
				ws.setName(server.getName());
				confS.setWebserver(ws);
				server.setConfiguration(confS);
				graph.getNode().add(server);

				// firewall
				Node firewall = new Node();
				firewall.setFunctionalType(FunctionalTypes.FIREWALL);
				String ips = instance.getIp();
				String[] ipParts = ips.split("\\.");
				int lastIp = Integer.parseInt(ipParts[3]) + 20;
				firewall.setName(ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + "." + lastIp);
				Configuration confF = new Configuration();
				confF.setDescription(instance.getIp());
				confF.setName(instance.getUid());
				Firewall fw = new Firewall();

				fw.setDefaultAction(ActionTypes.DENY);
				confF.setFirewall(fw);
				firewall.setConfiguration(confF);
				graph.getNode().add(firewall);

				// Neighbour server
				Neighbour prevFw = new Neighbour();
				prevFw.setName(firewall.getName());
				server.getNeighbour().add(prevFw);
				// Neighbour firewall
				Neighbour prevServer = new Neighbour();
				prevServer.setName(server.getName());
				firewall.getNeighbour().add(prevServer);
			}
		}

		for (Node src : graph.getNode()) {
			if (src.getFunctionalType().equals(FunctionalTypes.FIREWALL)) {
				for (Node dst : graph.getNode()) {
					if (dst.getFunctionalType().equals(FunctionalTypes.FIREWALL) && dst.getName() != src.getName()) {
						Neighbour srcNeighbour = new Neighbour();
						srcNeighbour.setName(dst.getName());
						src.getNeighbour().add(srcNeighbour);
						fwNObjectMap.put(dst.getName(), srcNeighbour);
					}
				}
			}
		}

		List<Node> filterNodeSet=new  ArrayList<Node>();
		HashSet<String> pairSet = new HashSet<String>();
		
		int ip = 1;
		for (Node srcFirewallNode : graph.getNode()) {
			if (srcFirewallNode.getFunctionalType().equals(FunctionalTypes.FIREWALL)) {
					for (Neighbour src_neighbour : srcFirewallNode.getNeighbour()) {
						Node dstFirewallNode = graph.getNode().stream()
								.filter(firewall -> firewall.getName().equals(src_neighbour.getName())
										&& firewall.getFunctionalType().equals(FunctionalTypes.FIREWALL))
								.findFirst().orElse(null);
						// neigbour firewall found
						
						if (dstFirewallNode != null && !pairSet.contains(srcFirewallNode.getName()+""+dstFirewallNode.getName())&&!pairSet.contains(dstFirewallNode.getName()+""+srcFirewallNode.getName())) {
							pairSet.add(srcFirewallNode.getName()+""+dstFirewallNode.getName());
							
							///srcFirewallNode dstFirewallNode
							
							// filtering link
							Node iFilterNode = new Node();
							iFilterNode.setFunctionalType(FunctionalTypes.FILTERINGLINK);
							iFilterNode.setName(10 + "." + 10 + "." + 10 + "." + (ip++));

							Configuration confF = new Configuration();
							confF.setDescription("a");
							confF.setName(srcFirewallNode.getName());

							Filteringlink filterConf = new Filteringlink();
							filterConf.setIPA(srcFirewallNode.getConfiguration().getDescription());
							filterConf.setIPB(dstFirewallNode.getConfiguration().getDescription());
							confF.setFilteringlink(filterConf);
							iFilterNode.setConfiguration(confF);
							
							filterNodeSet.add(iFilterNode);

							// add neigbors to filteringlink
							Neighbour filterNeighbour = new Neighbour();
							filterNeighbour.setName(srcFirewallNode.getName());
							iFilterNode.getNeighbour().add(filterNeighbour);
							Neighbour filterNeighbour2 = new Neighbour();
							filterNeighbour2.setName(dstFirewallNode.getName());
							iFilterNode.getNeighbour().add(filterNeighbour2);

							// add neigbors of filteringlink to firewalls
							Neighbour fwsrcneighbour = new Neighbour();
							fwsrcneighbour.setName(iFilterNode.getName());
							// srcFirewallNode.getNeighbour().add(fwsrcneighbour);
							if(nodeNeighborFilterMap.get(srcFirewallNode)==null) {
								ArrayList<Neighbour> list = new ArrayList<Neighbour>();
								list.add(fwsrcneighbour);
								nodeNeighborFilterMap.put(srcFirewallNode, list);
							}else nodeNeighborFilterMap.get(srcFirewallNode).add(fwsrcneighbour);

							Neighbour fwdstneighbour = new Neighbour();
							fwdstneighbour.setName(iFilterNode.getName());
							// dstFirewallNode.getNeighbour().add(fwdstneighbour);
							//nodeNeighborFilterMap.put(dstFirewallNode, fwdstneighbour);
							if(nodeNeighborFilterMap.get(dstFirewallNode)==null) {
								ArrayList<Neighbour> list = new ArrayList<Neighbour>();
								list.add(fwdstneighbour);
								nodeNeighborFilterMap.put(dstFirewallNode, list);
							}else nodeNeighborFilterMap.get(dstFirewallNode).add(fwdstneighbour);

						}
					}
			}
		}
		
		filterNodeSet.forEach(p->graph.getNode().add(p));
		
		for (Node src : graph.getNode()) {
			if(nodeNeighborFilterMap.get(src)!=null) {
				src.getNeighbour().addAll(nodeNeighborFilterMap.get(src));
			}
		}
		

		for (Node src : graph.getNode()) {
			if (src.getFunctionalType().equals(FunctionalTypes.FIREWALL)) {
				Iterator<Neighbour> iterator = src.getNeighbour().iterator();
				while (iterator.hasNext()) {
					Neighbour word = iterator.next();
					if (fwNObjectMap.containsKey(word.getName())) {
						iterator.remove();
					}
				}
			}
		}

		//System.out.println("Adding policies numer=" + policies.getSpec().getPolicies().size());
		
		for (Policy policy : policyGraph.getSpec().getPolicies()) {

			for (Node serverNodeSrc : graph.getNode()) {
				if (serverNodeSrc.getFunctionalType().equals(FunctionalTypes.WEBSERVER)
						&& serverNodeSrc.getConfiguration().getName().equals(policy.getFrom())) {
					for (Node serverNodeDst : graph.getNode()) {
						if (serverNodeDst.getFunctionalType().equals(FunctionalTypes.WEBSERVER)
								&& serverNodeDst.getConfiguration().getName().equals(policy.getTo())) {
							Property property = new Property();
							// ((city.getName() == null) ? "N/A" : city.getName());
							property.setName(policy.getAction().equals("drop") ? PName.ISOLATION_PROPERTY
									: PName.REACHABILITY_PROPERTY);
							property.setGraph(Math.abs((long) inf.getMetadata().getName().hashCode()) % 100);
							property.setSrc(serverNodeSrc.getName());
							property.setDst(serverNodeDst.getName());
							property.setSrcPort(serverNodeSrc.getConfiguration().getDescription());
						    property.setDstPort(serverNodeDst.getConfiguration().getDescription());
							nfv.getPropertyDefinition().getProperty().add(property);
						}
					}
				}

			}

		}

		// logger.info(stringWriter.toString());

		nfv.getGraphs().getGraph().add(graph);
		return nfv;
	}

	private static void createFiltering(Graph graph, int ip, Node src, String srcA, String dstB) {
		// filtering link
		Node filter = new Node();
		filter.setFunctionalType(FunctionalTypes.FILTERINGLINK);
		filter.setName(10 + "." + 10 + "." + 10 + "." + (ip));

		Configuration confF = new Configuration();
		confF.setDescription("a");
		confF.setName(src.getName());

		Filteringlink fw = new Filteringlink();

		fw.setIPA(srcA);

		fw.setIPB(dstB);
		confF.setFilteringlink(fw);
		filter.setConfiguration(confF);
		graph.getNode().add(filter);
	}

	public static InfrastructureInfo partialGraph(String file) {
		JAXBContext jc;
		InfrastructureInfo inf = null;
		try {
			jc = JAXBContext.newInstance("it.polito.verefoo.astrid.jaxb");
			Unmarshaller u = jc.createUnmarshaller();
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(new File("./xsd/astrid.xsd"));
			inf = (InfrastructureInfo) u.unmarshal(new FileInputStream(file));

		} catch (JAXBException | FileNotFoundException | SAXException e) {
			e.printStackTrace();
		}
		return inf;
	}

	public static String convertFileToString(String string) throws IOException {
		FileInputStream file = new FileInputStream(string);
		BufferedReader buf = new BufferedReader(new InputStreamReader(file));
		String line = buf.readLine();
		StringBuilder sb = new StringBuilder();

		while (line != null) {
			sb.append(line).append("\n");
			line = buf.readLine();
		}

		String fileAsString = sb.toString();
		return fileAsString;
	}

	public static PolicyGraph parsePolicies(String string) {
		
		Representer representer = new Representer();
		representer.getPropertyUtils().setSkipMissingProperties(true);
		Yaml yaml = new Yaml(representer);
		return yaml.loadAs(string, PolicyGraph.class);
		
	}

	
	private static void removeFilters(NFV result) {
		Iterator<Node> iterator = result.getGraphs().getGraph().get(0).getNode().iterator();
		while (iterator.hasNext()) {
			Node src = iterator.next();
			if (src.getFunctionalType().equals(FunctionalTypes.FILTERINGLINK)) {
				iterator.remove();
			}
			if (src.getFunctionalType().equals(FunctionalTypes.FIREWALL)) {
				Iterator<Neighbour> neiIterator = src.getNeighbour().iterator();
				while (neiIterator.hasNext()) {
					Neighbour nei = neiIterator.next();
					if(nei.getName().startsWith("10.10.10.")) {
						neiIterator.remove();
					}
				}
			}
		}
		for (Node src : result.getGraphs().getGraph().get(0).getNode()) {
			if (src.getFunctionalType().equals(FunctionalTypes.FIREWALL)) {
				for (Node dst : result.getGraphs().getGraph().get(0).getNode()) {
					if (dst.getFunctionalType().equals(FunctionalTypes.FIREWALL) && dst.getName() != src.getName()) {
						Neighbour srcNeighbour = new Neighbour();
						srcNeighbour.setName(dst.getName());
						src.getNeighbour().add(srcNeighbour);
					}
				}
			}
		}
		
	}
}
