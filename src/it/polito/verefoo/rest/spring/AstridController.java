package it.polito.verefoo.rest.spring;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.polito.verefoo.VerefooSerializer;
import it.polito.verefoo.astrid.jaxb.*;
import it.polito.verefoo.astrid.jaxb.InfrastructureInfo.Spec.Service;
import it.polito.verefoo.astrid.jaxb.InfrastructureInfo.Spec.Service.Instance;
import it.polito.verefoo.astrid.jaxb.InfrastructureInfo.Spec.Service.Port;
import it.polito.verefoo.astrid.jaxb.InfrastructureInfo.Spec.Service.SecurityComponent;
import it.polito.verefoo.astrid.yaml.MainYaml;
import it.polito.verefoo.astrid.yaml.PolicyGraph;
import it.polito.verefoo.jaxb.FunctionalTypes;
import it.polito.verefoo.jaxb.NFV;
import it.polito.verefoo.jaxb.Neighbour;
import it.polito.verefoo.jaxb.Node;
//mvn clean package && java -jar target\verifoo-0.0.1-SNAPSHOT.jar
//http://localhost:8083/controller/swagger-ui.html
//http://localhost:8083/swagger-ui.html

//8085 verifoo
//8083 controller

@Controller
public class AstridController {
	
	MainYaml main = new MainYaml();
	
	private Database db= Database.getInstance();

	@ApiOperation(value = "addGraph", notes = "Recieves policies")
	@RequestMapping(method = RequestMethod.POST, value = "/graph", consumes = "text/plain", produces = "text/plain")
	@ApiResponses(value = {
	    		@ApiResponse(code = 201, message = "Created"),
	    		@ApiResponse(code = 400, message = "Bad Request"),
	    		})
	@ResponseBody
	public String addGraph(@RequestBody String body) {
		if (body == null)
			return "---------- Verikube null body recieved";
		PolicyGraph policy = MainYaml.parsePolicies(body);
		db.addPolicy(policy);
		System.out.println("---------- Verikube Parsed policy: "+policy.getMetadata().getName());
		return "Verikube Successfully added: " + policy.getMetadata().getName();
	}
	
	
	
	@ApiOperation(value = "addEvent", notes = "Recieves Event  info sends back result")
	@RequestMapping(method = RequestMethod.POST, value = "/podevent", produces = "application/xml", consumes="application/xml")
	@ApiResponses(value = {
	    		@ApiResponse(code = 201, message = "Created"),
	    		@ApiResponse(code = 400, message = "Bad Request"),
	    		@ApiResponse(code = 404, message = "Policy is not defined for this graph"),
	    		})
	@ResponseBody
	public NFV addEvent(@RequestBody InfrastructureEvent body) throws ResourceNotFoundException {
		
		if(body==null || body.getGraphName()==null|| body.getEventData()==null|| body.getEventData().getName()==null) {
			System.out.println("---------- Verikube Event is null: " );
			throw new ResourceNotFoundException("Event is null ");
		}
		
		
		InfrastructureInfo info = db.getInfo(body.getGraphName());
		if(info == null) throw new ResourceNotFoundException("There is no graph corresponding to the graph name specified in this event");
		Service service = null;
		
		for (Service s : info.getSpec().getService()) {
			if(s.getName().equals(body.getEventData().getName())) service=s;
		}
		if(service == null) throw new ResourceNotFoundException("There is no service corresponding to this name ");
	
		
		if(body.getType().equals("new")) {
			ObjectFactory o = new ObjectFactory();
			Service newService = o.createInfrastructureInfoSpecService();
			Instance newSecInstance = o.createInfrastructureInfoSpecServiceInstance();
			SecurityComponent newSecComp = o.createInfrastructureInfoSpecServiceSecurityComponent();
			Port newPort = o.createInfrastructureInfoSpecServicePort();
			
			newSecComp.setName(service.getSecurityComponent().getName());
			newService.setSecurityComponent(newSecComp);
			
			newPort.setExposed(service.getPort().getExposed());
			newPort.setInternal(service.getPort().getInternal());
			newPort.setProtocol(service.getPort().getProtocol());
			newService.setPort(newPort);
			
			newSecInstance.setIp(body.getEventData().getIp());
			newSecInstance.setUid(body.getEventData().getUid());
			newService.getInstance().add(newSecInstance);
			newService.setName(service.getName());
			
			info.getSpec().getService().add(newService);
		}
		
		Instance temp = null;
		if(body.getType().equals("delete")) {
			for (Instance instance : service.getInstance()) {
				if(instance.getUid().equals(body.getEventData().getUid()))
					temp = instance;
			}
			if(temp!=null) service.getInstance().remove(temp);
			else {
				throw new ResourceNotFoundException("There is no instance corresponding to this uid ");
			}
		}
		
		NFV nfv = MainYaml.parseGraph(info,db.getPolicy(info.getMetadata().getName()));
		
		long startTime = System.currentTimeMillis();
		NFV result = solve(nfv);
		long endTime = System.currentTimeMillis();
		long spentTime = endTime - startTime;
		System.out.println("---------- Verikube spent ms: " + spentTime);
		return result;
	}

	@ApiOperation(value = "addDC", notes = "Recieves Infrastructure info sends back result")
	@RequestMapping(method = RequestMethod.POST, value = "/dc", produces = "application/xml", consumes="application/xml")
	@ApiResponses(value = {
	    		@ApiResponse(code = 201, message = "Created"),
	    		@ApiResponse(code = 400, message = "Bad Request"),
	    		@ApiResponse(code = 404, message = "Policy is not defined for this graph"),
	    		})
	@ResponseBody
	public NFV addInfrastructure(@RequestBody InfrastructureInfo body) throws ResourceNotFoundException {
		if (body == null || body.getMetadata() == null || body.getMetadata().getName() == null) {
			System.out.println("---------- Verikube Empty body InfrastructureInfo");
			return null;
		}
		System.out.println("---------- Verikube Obtained IN info: " + body.getMetadata().getName());
		
		if(!db.existsPolicy(body.getMetadata().getName())) {
			System.out.println("---------- Verikube Policy is not defined for this graph: "+body.getMetadata().getName() );
			throw new ResourceNotFoundException("Policy is not defined for this graph: " +  body.getMetadata().getName());
		}
		
		
		NFV nfv = MainYaml.parseGraph(body,db.getPolicy(body.getMetadata().getName()));
		db.addInfo(body);
		
		long startTime = System.currentTimeMillis();
		NFV result = solve(nfv);
		long endTime = System.currentTimeMillis();
		long spentTime = endTime - startTime;
		System.out.println("---------- Verikube spent ms: " + spentTime);
		
		return result;
	}



	private NFV solve(NFV nfv) {
		VerefooSerializer test = null;
		try {
			JAXBContext jc = null;

			StringWriter stringWriter = null;
			// create a JAXBContext capable of handling the generated classes
			jc = JAXBContext.newInstance("it.polito.verefoo.jaxb");
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "./xsd/nfvSchema.xsd");

			StringWriter stringWriter2 = new StringWriter();
			m.marshal(nfv, stringWriter2);
			//System.out.println("----------------------NFV before----------------------");
			//System.out.println(stringWriter2.toString());

			test = new VerefooSerializer(nfv);
			if (test.isSat()) {
				System.out.println("SAT");
				System.out.println("----------------------OUTPUT----------------------");
				stringWriter = new StringWriter();
				removeFilters(test.getResult());
				m.marshal(test.getResult(), System.out); // for debug purpose
				System.out.println("--------------------------------------------------");
			} else {
				System.out.println("UNSAT");
				System.out.println("----------------------OUTPUT----------------------");
				stringWriter = new StringWriter();
				m.marshal(test.getResult(), stringWriter);
				System.out.println(stringWriter.toString());
				System.out.println("--------------------------------------------------");
				System.exit(1);
			}

		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return test.getResult();
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
