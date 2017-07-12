
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.pathcomputation.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.PathEdge;
import it.nextworks.nido.path.PathStatus;
import it.nextworks.nido.pathcomputation.PathComputationListenerInterface;
import it.nextworks.nido.provisioning.topology.Link;
import it.nextworks.nido.provisioning.topology.Node;
import it.nextworks.nido.provisioning.topology.TopologyDbWrapper;
import it.nextworks.nido.provisioning.topology.TopologyManager;

public class DummyMdComputationAlgorithm extends AbstractMdComputationAlgorithm {
	
	private static final Logger log = LoggerFactory.getLogger(DummyMdComputationAlgorithm.class);
	
	private TopologyManager topologyManager;
	private TopologyDbWrapper topologyDbWrapper;

	public DummyMdComputationAlgorithm(TopologyManager topologyManager, TopologyDbWrapper topologyDbWrapper) {
		super(MdAlgorithmType.DUMMY);
		this.topologyManager = topologyManager;
		this.topologyDbWrapper = topologyDbWrapper;
	}

	@Override
	public void computeInterDomainPath(InterDomainPath path, PathComputationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		String sourceNode = path.getSourceEndPoint().getNodeId();
		String destinationNode = path.getDestinationEndPoint().getNodeId();
		log.debug("Computing interdomain path between " + sourceNode + " and " + destinationNode);
		
		String sourceDomain = path.getSourceEndPoint().getDomainId();
		String destinationDomain = path.getDestinationEndPoint().getDomainId();
		if (sourceDomain.equals(destinationDomain)) {
			log.debug("This is a request for an intra-domain path.");
			IntraDomainPath intraPath = new IntraDomainPath(null, path.getSourceEndPoint(), path.getDestinationEndPoint(), 
					path.getConnectionType(), path.getTrafficClassifier(), path.getTrafficProfile(), PathStatus.REQUESTED, sourceDomain, UUID.randomUUID().toString());
			List<IntraDomainPath> intraDomainPaths = new ArrayList<>();
			intraDomainPaths.add(intraPath);
			path.setIntraDomainPaths(intraDomainPaths);
		} else {
			log.debug("This is a request for a path between different domains.");
			SimpleWeightedGraph<String, DefaultWeightedEdge> graph = topologyManager.getWeightedGraph();
			log.debug("Got topology graph: " + graph.toString());
			DijkstraShortestPath<String, DefaultWeightedEdge> spfAlgorithm = new DijkstraShortestPath<>(graph);
			GraphPath<String, DefaultWeightedEdge> graphPath = spfAlgorithm.getPath(sourceNode, destinationNode);
			if (graphPath == null) {
				log.debug("Impossible to find path");
				listener.notifyPathComputationResult(path.getInterDomainPathId(), OperationResult.FAILED, path);
			}
			log.debug("Found graph path: " + graphPath.toString());

			List<IntraDomainPath> intraDomainPaths = translateGraphPath(graphPath, path);
			path.setIntraDomainPaths(intraDomainPaths);
		}
		log.debug("Resulting inter-domain path: " + path.toString());
		listener.notifyPathComputationResult(path.getInterDomainPathId(), OperationResult.COMPLETED, path);
	}
	
	private List<IntraDomainPath> translateGraphPath(GraphPath<String, DefaultWeightedEdge> graphPath, InterDomainPath idp) 
		throws GeneralFailureException {
		log.debug("Translating graph path to a list of intra-domain paths.");
		try {
			List<IntraDomainPath> intraDomainPaths = new ArrayList<>();
			List<DefaultWeightedEdge> links = graphPath.getEdgeList();
			if (links.size() != 5) {
				log.error("Unacceptable path. We expect a path crossing 2 DCs and 1 inter-DC domain.");
				throw new GeneralFailureException("Unacceptable path. We expect a path crossing 2 DCs and 1 inter-DC domain.");
			}

			List<String> vertices = graphPath.getVertexList();
			log.debug("List of vertices: " + vertices.toString());

			for (int i=1; i<vertices.size(); i+=2) {
				String srcNodeId = vertices.get(i-1);
				String domainId = null;
				String dstNodeId = vertices.get(i);
				
				log.debug("Processing intra-domain path between " + srcNodeId + " and " + dstNodeId);

				PathEdge sourceEndPoint;
				if ((i-1)==0) {
					log.debug("This is the first path.");
					sourceEndPoint = idp.getSourceEndPoint();
					domainId = idp.getSourceEndPoint().getDomainId();
				} else {
					String peerSrcNodeId = vertices.get(i-2);
					Node srcNode = topologyDbWrapper.getNode(srcNodeId);
					domainId = srcNode.getDomainId();
					Link l = srcNode.getLinkWithPeerNode(peerSrcNodeId);
					sourceEndPoint = new PathEdge(domainId, srcNodeId, l.getLocalPortId());
				}
				PathEdge destinationEndPoint;
				if (i==5) {
					log.debug("This is the last path.");
					destinationEndPoint = idp.getDestinationEndPoint();
				} else {
					String peerDstNodeId = vertices.get(i+1);
					Node dstNode = topologyDbWrapper.getNode(dstNodeId);
					Link l = dstNode.getLinkWithPeerNode(peerDstNodeId);
					destinationEndPoint = new PathEdge(domainId, dstNodeId, l.getLocalPortId());
				}

				IntraDomainPath intraDomainPath = new IntraDomainPath(null, 
						sourceEndPoint, 
						destinationEndPoint, 
						idp.getConnectionType(), 
						idp.getTrafficClassifier(), 
						idp.getTrafficProfile(), 
						PathStatus.REQUESTED, 
						domainId, 
						UUID.randomUUID().toString());
				intraDomainPaths.add(intraDomainPath);

				log.debug("Added intra-domain path: " + intraDomainPath.toString());
			}

			return intraDomainPaths;
		} catch (Exception e) {
			log.error("Error while translating graph path: " + e.getMessage());
			throw new GeneralFailureException("Error while translating graph path: " + e.getMessage());
		}
	}

}
