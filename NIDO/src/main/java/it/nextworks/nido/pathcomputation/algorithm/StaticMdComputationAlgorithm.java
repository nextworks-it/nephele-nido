
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.path.ConnectionType;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.PathEdge;
import it.nextworks.nido.path.PathStatus;
import it.nextworks.nido.pathcomputation.PathComputationListenerInterface;

public class StaticMdComputationAlgorithm extends AbstractMdComputationAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(StaticMdComputationAlgorithm.class);
	
	public StaticMdComputationAlgorithm() {
		super(MdAlgorithmType.STATIC);
	}

	@Override
	public void computeInterDomainPath(InterDomainPath path, PathComputationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		
		PathEdge srcEdge = path.getSourceEndPoint();
		PathEdge dstEdge = path.getDestinationEndPoint();
		
		String srcDc = srcEdge.getDomainId();
		String dstDc = dstEdge.getDomainId();
		
		int srcDcIndex = getDcIndex(srcDc);
		int dstDcIndex = getDcIndex(dstDc);
		
		String srcDcEdgeNode = getDcEdgeNode(srcDcIndex);
		String dstDcEdgeNode = getDcEdgeNode(dstDcIndex);
		
		String srcInterDcEdgeNode = getInterDomainEdgeNode(srcDcIndex);
		String dstInterDcEdgeNode = getInterDomainEdgeNode(dstDcIndex);
		
		List<IntraDomainPath> intraDomainPaths = new ArrayList<>();
		
		IntraDomainPath srcDcPath = new IntraDomainPath(null, 
				path.getSourceEndPoint(),
				new PathEdge(srcDc, srcDcEdgeNode, "4"),
				ConnectionType.POINT_TO_POINT,
				path.getTrafficClassifier(),
				path.getTrafficProfile(),
				PathStatus.REQUESTED,
				srcDc,
				path.getInterDomainPathId() + "_01");
		
		intraDomainPaths.add(srcDcPath);
		
		IntraDomainPath interDcPath = new IntraDomainPath(null, 
				new PathEdge("InterDC_01", srcInterDcEdgeNode, "1"),
				new PathEdge("InterDC_01", dstInterDcEdgeNode, "1"),
				ConnectionType.POINT_TO_POINT,
				path.getTrafficClassifier(),
				path.getTrafficProfile(),
				PathStatus.REQUESTED,
				srcDc,
				path.getInterDomainPathId() + "_02");
		
		intraDomainPaths.add(interDcPath);
		
		IntraDomainPath dstDcPath = new IntraDomainPath(null,
				new PathEdge(dstDc, dstDcEdgeNode, "4"),
				path.getDestinationEndPoint(),
				ConnectionType.POINT_TO_POINT,
				path.getTrafficClassifier(),
				path.getTrafficProfile(),
				PathStatus.REQUESTED,
				srcDc,
				path.getInterDomainPathId() + "_03");
		
		intraDomainPaths.add(dstDcPath);
		
		path.setIntraDomainPaths(intraDomainPaths);
		
		listener.notifyPathComputationResult(path.getInterDomainPathId(), OperationResult.COMPLETED, path);
	}
	
	public int getDcIndex(String dcDomainId) {
		if (dcDomainId.equals("DCN_01")) return 1;
		else if (dcDomainId.equals("DCN_02")) return 2;
		else if (dcDomainId.equals("DCN_03")) return 3;
		else {
			log.error("Unacceptable DC name");
			return 0;
		}
	}
	
	private String getDcEdgeNode(int dc) {
		switch (dc) {
		case 1:
			return "117010";
			
		case 2:
			return "127010";
		
		case 3:
			return "137010";
		
		default:
			log.error("Unacceptable DC number");
			return null;
		}
	}
	
	private String getInterDomainEdgeNode(int attachedDc) {
		switch (attachedDc) {
		case 1:
			return "1";
			
		case 2:
			return "4";
		
		case 3:
			return "7";
		
		default:
			log.error("Unacceptable attached DC number");
			return null;
		}
	}

}
