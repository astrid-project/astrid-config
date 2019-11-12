package it.polito.verigraph.functions;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.DatatypeExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Optimize;

import it.polito.verefoo.allocation.AllocationNode;
import it.polito.verigraph.solver.NetContext;

/** Represents a Forwarder
 *
 */
public class FilteringLinkFunction extends GenericFunction{
	DatatypeExpr forwarder;
	/**
	 * Public constructor for the Forwarder
	 * @param source It is the Allocation Node on which the forwarder is put
	 * @param ctx It is the Z3 Context in which the model is generated
	 * @param nctx It is the NetContext object to which constraints are sent
	 */
	public FilteringLinkFunction(AllocationNode source, Context ctx, NetContext nctx) {
		forwarder = source.getZ3Name();
		this.source = source;
		this.ctx = ctx;
		this.nctx = nctx;
		constraints = new ArrayList<BoolExpr>();
		isEndHost = false;
	}


    /**
     * This method creates the forwarding rules for the forwarder.
     * Since it does not provide any filtering behaviour, the forwarders sends all the received packets.
     */
    public void filteringLinkSendRules (){
    	Expr p_0 = ctx.mkConst(forwarder+"_forwarder_send_p_0", nctx.packetType);
    	String ipA = source.getNode().getConfiguration().getFilteringlink().getIPA();
    	String ipB = source.getNode().getConfiguration().getFilteringlink().getIPB();
    	BoolExpr filter = ctx.mkOr(
				ctx.mkAnd(nctx.equalIpToIntArray(nctx.functionsMap.get("src").apply(p_0), nctx.getIpFromString(ipA)), nctx.equalIpToIntArray(nctx.functionsMap.get("dest").apply(p_0), nctx.getIpFromString(ipB))),
				ctx.mkAnd(nctx.equalIpToIntArray(nctx.functionsMap.get("src").apply(p_0), nctx.getIpFromString(ipB)), nctx.equalIpToIntArray(nctx.functionsMap.get("dest").apply(p_0), nctx.getIpFromString(ipA)))		
				);
    	
    	/*
    	 * for each leftHop lH, for each p_0,
    	 * recv(lH, forwader, p_0) --> AND (forwader, nextHops, p_0)
    	 * 
    	 * Basically, for each p_0 received from a leftHop,
    	 * it is sent to each possible rightHops depending on that specific leftHop.
    	 */
    	for(Map.Entry<AllocationNode, Set<AllocationNode>> entry : source.getLeftHops().entrySet()) {
    		for(AllocationNode dest : entry.getValue()) {
    			BoolExpr send = (BoolExpr) nctx.send.apply(forwarder, dest.getZ3Name(), p_0);
        		BoolExpr recv= (BoolExpr) nctx.recv.apply(entry.getKey().getZ3Name(), forwarder, p_0);
      			constraints.add(ctx.mkForall(new Expr[] { p_0 },
    							ctx.mkImplies(ctx.mkAnd((BoolExpr) recv, filter),
    							 send), 
    							1, null, null, null, null));
      			
      			constraints.add(ctx.mkForall(new Expr[] { p_0 },
    					ctx.mkImplies(ctx.mkAnd((BoolExpr) recv, ctx.mkNot(filter)),
    					 ctx.mkNot(send)), 
    					1, null, null, null, null));
      			
      			
      			constraints.add(ctx.mkForall(new Expr[] { p_0 },
    					ctx.mkImplies(send, ctx.mkAnd(recv, filter)), 
    					1, null, null, null, null));
      			
      	}
    		
  			
  			
  		}
    	
    	
    	/*
    	 * for each rightHop rH, for each p_0,
    	 * send(forwader, rH, p_0) --> OR (leftHops, forwader, p_0)
    	 * 
    	 * Basically, for each p_0 sent to a rightHop,
    	 * it must have been received from at least a leftHop depending on that specific rightHop.
    	 */
    	for(Map.Entry<AllocationNode, Set<AllocationNode>> entry : source.getRightHops().entrySet()){
    		BoolExpr enumerateRecv = createOrRecv(entry, p_0, forwarder);
  			BoolExpr send = (BoolExpr) nctx.send.apply(forwarder, entry.getKey().getZ3Name(), p_0);
  	 	/*	constraints.add(ctx.mkForall(new Expr[] {p_0 }, 
  	  					ctx.mkImplies(ctx.mkAnd((BoolExpr) send, filter),
  	  									enumerateRecv
  	  										), 1, null, null, null, null)); */
  	 		
  	 		/*constraints.add(ctx.mkForall(new Expr[] { p_0 },
					ctx.mkImplies((BoolExpr) send,
							ctx.mkOr(
									ctx.mkAnd(nctx.equalIpToIntArray(nctx.functionsMap.get("src").apply(p_0), nctx.getIpFromString(ipA)), nctx.equalIpToIntArray(nctx.functionsMap.get("dest").apply(p_0), nctx.getIpFromString(ipB))),
									ctx.mkAnd(nctx.equalIpToIntArray(nctx.functionsMap.get("src").apply(p_0), nctx.getIpFromString(ipB)), nctx.equalIpToIntArray(nctx.functionsMap.get("dest").apply(p_0), nctx.getIpFromString(ipA)))		
									)
							 ), 
							1, null, null, null, null));*/
  	 		

  		}
    	
    }
    
 
	/**
	 * This method allows to wrap the method which adds the constraints inside Z3 solver
	 * @param solver Istance of Z3 solver
	 */
	@Override
	public void addContraints(Optimize solver) {
		BoolExpr[] constr = new BoolExpr[constraints.size()];
	    solver.Add(constraints.toArray(constr));
	}

  
}

