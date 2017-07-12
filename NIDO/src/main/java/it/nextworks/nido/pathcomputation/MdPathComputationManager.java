
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.pathcomputation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.exceptions.AlgorithmNotAvailableException;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.engine.MultiDomainConnectivityManager;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.pathcomputation.algorithm.DummyMdComputationAlgorithm;
import it.nextworks.nido.pathcomputation.algorithm.MdComputationAlgorithmInterface;
import it.nextworks.nido.pathcomputation.algorithm.StaticMdComputationAlgorithm;
import it.nextworks.nido.provisioning.topology.TopologyDbWrapper;
import it.nextworks.nido.provisioning.topology.TopologyManager;

@Service
public class MdPathComputationManager implements MdPathComputationInterface, PathComputationListenerInterface {

	private static final Logger log = LoggerFactory.getLogger(MdPathComputationManager.class);
	
	@Autowired
	MultiDomainConnectivityManager mdcManager;
	
	@Autowired
	private TopologyDbWrapper topologyDbWrapper;
	
	@Autowired
	private TopologyManager topologyManager;
	
	@Value("${nido.algorithm}")
	private String algorithmType;
	
	public MdPathComputationManager() {	}
	
	@Override
	public void computeInterDomainPath(InterDomainPath path)
			throws EntityNotFoundException, GeneralFailureException {
		log.debug("Received request to compute path " + path.getInterDomainPathId());
		try {
			MdComputationAlgorithmInterface algorithm = loadAlgorithm();
			algorithm.computeInterDomainPath(path, this);
		} catch (AlgorithmNotAvailableException e) {
			log.error("Algorithm not found. Impossible to compute path.");
			mdcManager.notifyPathComputationResult(path.getInterDomainPathId(), OperationResult.FAILED, path);
		}
	}
	
	@Override
	public void notifyPathComputationResult(String interDomainPathId,
			OperationResult result,
			InterDomainPath path) throws EntityNotFoundException {
		log.debug("Received algorithm response for inter-domain path " + interDomainPathId);
		mdcManager.notifyPathComputationResult(interDomainPathId, result, path);
	}

	private MdComputationAlgorithmInterface loadAlgorithm() throws AlgorithmNotAvailableException {
		//TODO: update with other types when available
		//TODO: at the moment in configuration. Maybe it should be managed with some policies...
		if (this.algorithmType.equals("DUMMY")) {
			return new DummyMdComputationAlgorithm(topologyManager, topologyDbWrapper);
		} else if (this.algorithmType.equals("STATIC")) { 
			return new StaticMdComputationAlgorithm();
		}else {
			log.error("Unable to find a suitable algorithm");
			throw new AlgorithmNotAvailableException();
		}
	}
	
}
