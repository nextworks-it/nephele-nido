
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.engine;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.enums.PathLifecycleAction;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.engine.message.InterDomainPathSetupMessage;
import it.nextworks.nido.engine.message.InterDomainPathTearDownMessage;
import it.nextworks.nido.engine.message.IntraDomainProvisioningResultMessage;
import it.nextworks.nido.engine.message.PathComputationResultMessage;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.PathDbWrapper;
import it.nextworks.nido.path.PathStatus;
import it.nextworks.nido.pathcomputation.MdPathComputationManager;
import it.nextworks.nido.provisioning.ProvisioningManager;
import it.nextworks.nido.provisioning.topology.TopologyManager;

public class MdConnectionManager {
	
	private static final Logger log = LoggerFactory.getLogger(MdConnectionManager.class);
	
	private String interDomainConnectionId;

	private MultiDomainConnectivityManager mdcManager;
	
	private PathDbWrapper pathDbWrapper;
	
	private InterDomainPath path;
	
	private PathStatus pathStatus;
	
	private MdPathComputationManager computationManager;
	
	private ProvisioningManager provisioningManager;
	
	private TopologyManager topologyManager;
	
	public MdConnectionManager(String interDomainConnectionId,
			MultiDomainConnectivityManager mdcManager,
			PathDbWrapper pathDbWrapper,
			MdPathComputationManager computationManager,
			ProvisioningManager provisioningManager,
			TopologyManager topologyManager,
			InterDomainPath path) {
		this.interDomainConnectionId = interDomainConnectionId;
		this.mdcManager = mdcManager;
		this.pathDbWrapper = pathDbWrapper;
		this.computationManager = computationManager;
		this.provisioningManager = provisioningManager;
		this.topologyManager = topologyManager;
		this.path = path;
		pathStatus = PathStatus.REQUESTED;
	}
	
	public void setupInterDomainPath(InterDomainPathSetupMessage message) {
		log.debug("Received request to setup path " + message.getInterDomainPathId());
		if (this.pathStatus != PathStatus.REQUESTED) {
			log.error("Received path computation result in wrong status.");
			notifyPathFailure();
			return;
		}
		try {
			computationManager.computeInterDomainPath(message.getInterDomainPath());
			log.debug("Invoked path computation.");
		} catch (Exception e) {
			log.error("Error while invoking computation manager: " + e.getMessage());
			notifyPathFailure();
		}
	}
	
	public void tearDownInterDomainPath(InterDomainPathTearDownMessage message) {
		String interDomainPathId = message.getInterDomainPathId();
		log.debug("Received request to teardown path " + interDomainPathId);
		if (this.pathStatus != PathStatus.ACTIVE) {
			log.error("Received tear-down request in wrong status. Ignoring it.");
			return;
		}
		setPathStatus(PathStatus.TERMINATING);
		try {
			InterDomainPath interPath = pathDbWrapper.retrieveInterDomainPath(interDomainPathId);
			List<IntraDomainPath> intraDomainPaths = interPath.getIntraDomainPaths();
			log.debug("Found " + intraDomainPaths.size() + " paths to be removed");
			for (IntraDomainPath ip : intraDomainPaths) {
				String intraDomainPathId = ip.getInternalId();
				log.debug("Removing intra domain path " + intraDomainPathId);
				pathDbWrapper.modifyIntraDomainPathStatus(intraDomainPathId, PathStatus.TERMINATING);
				provisioningManager.teardownIntraDomainPath(interDomainPathId, intraDomainPathId, mdcManager);
				log.debug("Sent tear down request to provisioning manager.");
			}
			topologyManager.updateWeightedGraph(PathLifecycleAction.TEARDOWN, interPath);
			log.debug("Topology manager updated.");
		}
		catch (EntityNotFoundException e) {
			log.error("Entity not found in DB: " + e.getMessage());
			notifyPathFailure();
		}
		catch (GeneralFailureException e) {
			log.error("General exception when tearing down an intra domain path.");
			notifyPathFailure();
		}
	}
	
	public void notifyPathComputationResult(PathComputationResultMessage message) {
		log.debug("Received notification with path computation result for path " + message.getInterDomainPathId() + " with result " + message.getResult().toString());
		if (this.pathStatus != PathStatus.REQUESTED) {
			log.error("Received path computation result in wrong status.");
			notifyPathFailure();
			return;
		}
		if (message.getResult().equals(OperationResult.COMPLETED)) {
			setPathStatus(PathStatus.ESTABLISHING);
			//Contact provisioning manager
			List<IntraDomainPath> intraDomainPath = message.getInterDomainPath().getIntraDomainPaths();
			if (intraDomainPath.size() == 0) {
				log.error("Received path computation result without intra domain paths.");
				notifyPathFailure();
				return;
			}
			List<IntraDomainPath> currentIntraDomainPaths = new ArrayList<>();
			for (IntraDomainPath idp : intraDomainPath) {
				try {
					String intraDomainPathId = provisioningManager.setupIntraDomainPath(interDomainConnectionId, idp, mdcManager);
					log.debug("Sent request to provisioning manager to establish a new intra-domain path. Returned ID: " + intraDomainPathId);
					IntraDomainPath cIdp = new IntraDomainPath(null, 
							idp.getSourceEndPoint(), 
							idp.getDestinationEndPoint(), 
							idp.getConnectionType(), 
							idp.getTrafficClassifier(), 
							idp.getTrafficProfile(), 
							PathStatus.SCHEDULED, 
							idp.getDomainId(),
							intraDomainPathId);
					currentIntraDomainPaths.add(cIdp);
					pathDbWrapper.createIntraDomainPath(interDomainConnectionId, cIdp);
				} catch (Exception e) {
					log.error("Error while requesting intra-domain path provisioning: " + e.getMessage());
					notifyPathFailure();
					return;
				}
			}
			path.setIntraDomainPaths(currentIntraDomainPaths);
		} else {
			log.error("Path computation failed.");
			notifyPathFailure();
		}
	}
	
	private void notifyIntraDomainPathSetup(IntraDomainProvisioningResultMessage message) throws Exception {
		if (this.pathStatus != PathStatus.ESTABLISHING) {
			log.error("Received path provisioning notification in wrong status.");
			notifyPathFailure();
			return;
		}
		if (message.getResult() != OperationResult.COMPLETED) {
			log.error("Provisioning has failed");
			pathDbWrapper.modifyIntraDomainPathStatus(message.getIntraDomainPathId(), PathStatus.FAILED);
			//TODO: Here it should invoke the tear-down of the created path. Implement rollback mechanisms.
			notifyPathFailure();
			return;
		} else {
			log.debug("Intra domain path " + message.getIntraDomainPathId() + " correctly established.");
			pathDbWrapper.modifyIntraDomainPathStatus(message.getIntraDomainPathId(), PathStatus.ACTIVE);
			if (pathDbWrapper.isAllIntraDomainPathsInStatus(interDomainConnectionId, PathStatus.ACTIVE)) {
				log.debug("All the intra-domain paths for inter-domain path " + interDomainConnectionId + " has been established.");
				setPathStatus(PathStatus.ACTIVE);
				log.debug("Inter-domain path " + interDomainConnectionId + " is now active.");
				topologyManager.updateWeightedGraph(PathLifecycleAction.SETUP, path);
			} else {
				log.debug("Some intra-domain path for inter-domain path " + interDomainConnectionId + " still need to be established. Waiting for other notifications.");
			}
		}
	}
	
	private void notifyIntraDomainPathTeardown(IntraDomainProvisioningResultMessage message) throws Exception {
		String intraDomainPathId = message.getIntraDomainPathId();
		if (this.pathStatus != PathStatus.TERMINATING) {
			log.error("Received path tear down notification in wrong status.");
			notifyPathFailure();
			return;
		}
		if (message.getResult() != OperationResult.COMPLETED) {
			log.error("Teardown of intra domain path " + intraDomainPathId + " has failed. Continuing the procedure in any case.");
		} else { 
			log.debug("Intra domain path " + message.getIntraDomainPathId() + " correctly removed.");
		}
		pathDbWrapper.modifyIntraDomainPathStatus(message.getIntraDomainPathId(), PathStatus.DELETED);
		if (pathDbWrapper.isAllIntraDomainPathsInStatus(interDomainConnectionId, PathStatus.DELETED)) {
			log.debug("All the intra-domain paths for inter-domain path " + interDomainConnectionId + " have been removed.");
			setPathStatus(PathStatus.DELETED);
			log.debug("Inter-domain path " + interDomainConnectionId + " is now terminated.");
		} else {
			log.debug("Some intra-domain path for inter-domain path " + interDomainConnectionId + " still need to be removed. Waiting for other notifications.");
		}
		
	}
	
	public void notifyIntraDomainPathProvisioning(IntraDomainProvisioningResultMessage message) {
		log.debug("Received notification for intra-domain path provisioning for intra-domain path " + message.getInterDomainPathId());
		try {
			if (message.getPathLifecycleAction() == PathLifecycleAction.SETUP) {
				notifyIntraDomainPathSetup(message);
			} else if (message.getPathLifecycleAction() == PathLifecycleAction.TEARDOWN) {
				notifyIntraDomainPathTeardown(message);
			}
		} catch (Exception e) {
			log.debug("Error: " + e.getMessage());
		}
		
	}
	
	private void setPathStatus(PathStatus pathStatus) {
		this.pathStatus = pathStatus;
		try {
			pathDbWrapper.modifyInterDomainPathStatus(interDomainConnectionId, pathStatus);
		} catch (EntityNotFoundException e1) {
			log.error("Impossible to update path status in DB");
		}
	}
	
	private void notifyPathFailure() {
		//TODO: should I also remove the MD connection manager from the MD connectivity manager?
		setPathStatus(PathStatus.FAILED);
	}

}
