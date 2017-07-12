
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.topology;

import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nextworks.nido.common.enums.PathLifecycleAction;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.PathDbWrapper;
import it.nextworks.nido.path.PathStatus;

@Service
public class TopologyManager {
	
	private static final Logger log = LoggerFactory.getLogger(TopologyManager.class);
	
	public static final double INTRA_ACCESS_DOMAIN_EDGE_WEIGHT = 1;
	public static final double INTRA_CORE_DOMAIN_EDGE_WEIGHT = 10;
	public static final double INTER_DOMAIN_EDGE_WEIGHT = 20;
	
	@Autowired
	private TopologyDbWrapper topologyDbWrapper;
	
	@Autowired
	PathDbWrapper pathDbWrapper;
	
	private SimpleWeightedGraph<String, DefaultWeightedEdge> weightedGraph;

	public TopologyManager() {	}
	
	/**
	 * Method to re-build the weighted graph.
	 * This method is invoked when a domain is added or removed from the topology DB wrapper.
	 * It must take into account the established paths. 
	 * 
	 */
	public void rebuildWeightedGraph() {
		log.debug("Re-building topology weighted graph");
		weightedGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		log.debug("Building sub-graph related to access domains.");
		List<Domain> accessDomains = topologyDbWrapper.getAllAccessDomains();
		for (Domain ad : accessDomains) {
			buildAccessDomainSubWeightedGraph(ad);
		}
		log.debug("Sub-graph related to access domains built");
		
		log.debug("Building sub-graph related to core domains.");
		List<Domain> coreDomains = topologyDbWrapper.getAllCoreDomains();
		for (Domain cd : coreDomains) {
			buildCoreDomainSubWeightedGraph(cd);
		}
		log.debug("Sub-graph related to core domains built");
		
		log.debug("Updating inter-domain edges with path-dependent weights.");
		List<InterDomainPath> idps = pathDbWrapper.retrieveAllInterDomainPaths();
		for (InterDomainPath idp : idps) {
			if (idp.getPathStatus() == PathStatus.ACTIVE) {
				updateWeightedGraph(PathLifecycleAction.SETUP, idp);
			}
		}
		log.debug("Updated inter-domain edges with path-dependent weights.");
		
		log.info("Topology weighted graph re-built");
	}
	
	/**
	 * This method build a portion of the weighted graph that corresponds
	 * to an access domain. 
	 * An access domain is represented through a graph where all the hosts are
	 * connected to each one of the nodes with an edge of default weight.
	 * 
	 * @param domain access domain to be added in the graph
	 */
	private void buildAccessDomainSubWeightedGraph(Domain domain) {
		log.debug("Building sub-graph related to access domain " + domain.getDomainId());
		List<Node> nodes = domain.getNodes();
		for (Node n : nodes) {
			String nodeId = n.getNodeId();
			weightedGraph.addVertex(nodeId);
			log.debug("Added vertex for node " + nodeId);
		}
		List<Host> hosts = domain.getHosts();
		for (Host h : hosts) {
			String hostId = h.getNodeId();
			weightedGraph.addVertex(hostId);
			log.debug("Added vertex for host " + hostId);
			for (Node n : nodes) {
				String edgeId = n.getNodeId();
				weightedGraph.addEdge(hostId, edgeId);
				weightedGraph.setEdgeWeight(weightedGraph.getEdge(hostId, edgeId), INTRA_ACCESS_DOMAIN_EDGE_WEIGHT);
				log.debug("Added edge between " + hostId + " and " + edgeId + " with weight " + INTRA_ACCESS_DOMAIN_EDGE_WEIGHT);
			}
		}
		log.debug("Sub-graph related to access domain " + domain.getDomainId() + " built.");
	}
	
	/**
	 * This method build a portion of the weighted graph that corresponds
	 * to a core domain. 
	 * A core domain is represented through a graph where all the nodes are
	 * interconnected each other through a full mesh of edges with a default 
	 * weight.
	 * The method also adds the edges related to the inter-domain links, if the
	 * peer domain is available in the topology. These links are added with a 
	 * default weight, since they will be updated with the correct one later on. 
	 * 
	 * @param domain core domain to be added in the graph
	 */
	private void buildCoreDomainSubWeightedGraph(Domain domain) {
		log.debug("Building sub-graph related to core domain " + domain.getDomainId());
		List<Node> nodes = domain.getNodes();
		for (Node n : nodes) {
			String nodeId = n.getNodeId();
			weightedGraph.addVertex(nodeId);
			log.debug("Added vertex for node " + nodeId);
		}
		for  (int i = 0; i < nodes.size()-1; i++) {
			for(int j = i+1; j< nodes.size(); j++) {
				String src = nodes.get(i).getNodeId();
				String dst = nodes.get(j).getNodeId();
				weightedGraph.addEdge(src, dst);
				weightedGraph.setEdgeWeight(weightedGraph.getEdge(src, dst), INTRA_CORE_DOMAIN_EDGE_WEIGHT);
				log.debug("Added edge between " + src + " and " + dst + " with weight " + INTRA_CORE_DOMAIN_EDGE_WEIGHT);
			}
		}
		log.debug("Added all the intra-core-domain edges.");
		for (Node n : nodes) {
			String localNodeId = n.getNodeId();
			List<Link> links = n.getLinks();
			for (Link l : links) {
				String peerDomainId = l.getPeerDomainId();
				try {
					Domain peerDomain = topologyDbWrapper.getDomain(peerDomainId);
					String peerNodeId = l.getPeerNodeId();
					weightedGraph.addEdge(localNodeId, peerNodeId);
					weightedGraph.setEdgeWeight(weightedGraph.getEdge(localNodeId, peerNodeId), INTER_DOMAIN_EDGE_WEIGHT);
					log.debug("Added edge between " + localNodeId + " and " + peerNodeId + " with weight " + INTER_DOMAIN_EDGE_WEIGHT);
				} catch (EntityNotFoundException e) {
					//peer domain not yet added in topology -> no inter-domain link to be added in the graph
				}
			}
		}
		log.debug("Added all the inter-domain edges with default weight. Updating weights based on established paths.");
		log.debug("Sub-graph related to core domain " + domain.getDomainId() + " built.");
	}
	
	
	
	
	
	/**
	 * Method to update the weighted graph.
	 * This method is invoked when an inter-domain path is created or removed.
	 * This method does not rebuild the entire graph, but it just updates the 
	 * weights of the inter-domain edges.
	 * 
	 * @param pathAction
	 * @param path
	 */
	public void updateWeightedGraph(PathLifecycleAction pathAction, InterDomainPath path) {
		if (weightedGraph == null) {
			rebuildWeightedGraph();
		}
		int bw = path.getTrafficProfile().getBandwidth();
		
		List<IntraDomainPath> intraPaths = path.getIntraDomainPaths();
		//Note: at the moment it considers a single core domain in the middle of two access domains. We assume that
		//the domains involved in the paths are available in the topology...
		
		String srcAccessEdgeId = intraPaths.get(0).getDestinationEndPoint().getNodeId();
		String srcCoreEdgeId = intraPaths.get(1).getSourceEndPoint().getNodeId();
		String dstCoreEdgeId = intraPaths.get(1).getDestinationEndPoint().getNodeId();
		String dstAccessEdgeId = intraPaths.get(2).getSourceEndPoint().getNodeId();
		
		Double currentWeight1 = weightedGraph.getEdgeWeight(weightedGraph.getEdge(srcCoreEdgeId, srcAccessEdgeId));
		Double updatedWeight1 = currentWeight1;
		if (pathAction == PathLifecycleAction.SETUP) {
			updatedWeight1 += bw;
		} else {
			updatedWeight1 -= bw;
		}
		weightedGraph.setEdgeWeight(weightedGraph.getEdge(srcCoreEdgeId, srcAccessEdgeId), updatedWeight1);
		log.debug("Set weight for edge between " + srcCoreEdgeId + " and " + srcAccessEdgeId + " to " + updatedWeight1);
		
		Double currentWeight2 = weightedGraph.getEdgeWeight(weightedGraph.getEdge(dstCoreEdgeId, dstAccessEdgeId));
		Double updatedWeight2 = currentWeight2;
		if (pathAction == PathLifecycleAction.SETUP) {
			updatedWeight2 += bw;
		} else {
			updatedWeight2 -= bw;
		}
		weightedGraph.setEdgeWeight(weightedGraph.getEdge(dstCoreEdgeId, dstAccessEdgeId), updatedWeight2);
		log.debug("Set weight for edge between " + dstCoreEdgeId + " and " + dstAccessEdgeId + " to " + updatedWeight2);
	}

	/**
	 * @return the weightedGraph
	 */
	public SimpleWeightedGraph<String, DefaultWeightedEdge> getWeightedGraph() {
		return weightedGraph;
	}

}
