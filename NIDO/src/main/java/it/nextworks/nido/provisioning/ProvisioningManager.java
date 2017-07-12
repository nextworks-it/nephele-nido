
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.enums.PathLifecycleAction;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.engine.PathNotificationListenerInterface;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.provisioning.plugin.DummyProvisioningPlugin;
import it.nextworks.nido.provisioning.plugin.JuliusPlugin;
import it.nextworks.nido.provisioning.plugin.OceaniaPlugin;
import it.nextworks.nido.provisioning.plugin.ProvisioningPlugin;
import it.nextworks.nido.provisioning.topology.Domain;
import it.nextworks.nido.provisioning.topology.TopologyDbWrapper;
import it.nextworks.nido.provisioning.topology.TopologyManager;

@Service
public class ProvisioningManager {
	
	private static final Logger log = LoggerFactory.getLogger(ProvisioningManager.class);
	
	//Key: domain ID; Value: plugin
	private Map<String, ProvisioningPlugin> domainsPluginMap = new HashMap<>();
	
	//Key: path ID; Value: domain ID
	private Map<String,String> intraDomainPathsMap = new HashMap<>();
	
	@Autowired
	private TopologyDbWrapper topologyDbWrapper;
	
	@Autowired
	private TopologyManager topologyManager;
	
	public ProvisioningManager() {	}
	
	@PostConstruct
	public void init() {
		log.debug("Initializing provisioning manager");
		List<Domain> domains = topologyDbWrapper.getAllDomains();
		for (Domain d : domains) {
			try {
				addDomainPlugin(d, true);
			} catch (Exception e) {
				log.warn("Malformed domain. Skipping.");
				continue;
			}
		}
		//TODO: read from DB the paths
		log.debug("Provisioning manager initialized");
	}
	
	public synchronized void addDomainPlugin(Domain domain, boolean initPhase) throws GeneralFailureException {
		log.debug("Adding domain plugin in provisioning manager.");
		switch (domain.getDomainType()) {
		case PLUGIN_OCEANIA: {
			OceaniaPlugin oceaniaPlugin = new OceaniaPlugin(domain.getDomainId(), domain.getUrl());
			domainsPluginMap.put(domain.getDomainId(), oceaniaPlugin);
			log.debug("OCEANIA plugin added in provisioning manager");
			if (!initPhase) {
				readTopology(oceaniaPlugin);
			}
			break;
		}

		case PLUGIN_JULIUS: {
			JuliusPlugin juliusPlugin = new JuliusPlugin(domain.getDomainId(), domain.getUrl());
			domainsPluginMap.put(domain.getDomainId(), juliusPlugin);
			log.debug("JULIUS plugin added in provisioning manager");
			if (!initPhase) {
				readTopology(juliusPlugin);
			}
			break;
		}
		
		case PLUGIN_DUMMY: {
			DummyProvisioningPlugin dummyPlugin = new DummyProvisioningPlugin(domain.getDomainId(), domain.getUrl());
			domainsPluginMap.put(domain.getDomainId(), dummyPlugin);
			log.debug("Dummy plugin added in provisioning manager");
			if (!initPhase) {
				readTopology(dummyPlugin);
			}
			break;
		}
			
		default: {
			log.error("Plugin type not acceptable");
			throw new GeneralFailureException("Plugin type not acceptable");
		}
		}
		topologyManager.rebuildWeightedGraph();
	}
	
	public synchronized void removeDomainPlugin(String domainId) throws EntityNotFoundException {
		log.debug("Removing plugin for domain " + domainId + " from provisioning manager");
		if (domainsPluginMap.containsKey(domainId)) {
			domainsPluginMap.remove(domainId);
			log.debug("Domain plugin removed.");
			topologyManager.rebuildWeightedGraph();
		} else {
			log.debug("Domain plugin not found");
			throw new EntityNotFoundException("Domain plugin not found");
		}
	}
	
	private void readTopology(ProvisioningPlugin plugin) throws GeneralFailureException {
		log.debug("Reading topology from domain " + plugin.getDomainId());
		Domain domain = plugin.readTopology();
		log.debug("Retrieved domain topology from plugin: " + domain.toString());
		try {
			topologyDbWrapper.addHostsToDomain(plugin.getDomainId(), domain.getHosts());
			topologyDbWrapper.addNodesToDomain(plugin.getDomainId(), domain.getNodes());
			log.debug("Topology DB updated for domain " + plugin.getDomainId());
		} catch (EntityNotFoundException e) {
			log.error("Unable to save domain topology in DB: " + e.getMessage());
			throw new GeneralFailureException("Unable to save domain topology in DB: " + e.getMessage());
		}
	}
	
	/**
	 * Setups an intra-domain path
	 * 
	 * @param interDomainPathId ID of the inter-domain path the intra-domain path belongs to
	 * @param path description of the path to be created
	 * @param listener listener that must be notified about the result of the operation
	 * @return the unique ID of the intra-domain path
	 * @throws EntityNotFoundException if the domain or one of its resources is not found
	 * @throws GeneralFailureException if the operation fails
	 */
	public String setupIntraDomainPath(String interDomainPathId, IntraDomainPath path, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		log.debug("Received request to create a new path: \n" + path.toString());
		String domainId = path.getDomainId();
		if (domainsPluginMap.containsKey(domainId)) {
			//Important Note: here we are delegating the creation of single ID to each plugin... not the best solution
			String intraDomainPathId;
			intraDomainPathId = domainsPluginMap.get(domainId).setupIntraDomainPath(interDomainPathId, path, listener);
			log.debug("Sent request for intra-domain path to the domain plugin.");
			intraDomainPathsMap.put(intraDomainPathId, domainId);
			log.debug("Internal map updated");
			return intraDomainPathId;
		} else throw new EntityNotFoundException("Domain " + domainId + " not configured.");
	}
	
	/**
	 * Tear-downs an intra-domain path
	 * 
	 * @param interDomainPathId ID of the inter-domain path the intra-domain path belongs to
	 * @param pathId ID of the path to be torn down
	 * @param listener listener that must be notified about the result of the operation
	 * @throws EntityNotFoundException if the path or the domain is not found
	 * @throws GeneralFailureException if the operation fails
	 */
	public void teardownIntraDomainPath(String interDomainPathId, String pathId, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		log.debug("Received request to teardown path " + pathId);
		if (intraDomainPathsMap.containsKey(pathId)) {
			String domainId = intraDomainPathsMap.get(pathId);
			if (domainsPluginMap.containsKey(domainId)) {
				try {
					domainsPluginMap.get(domainId).teardownIntraDomainPath(interDomainPathId, pathId, listener);
				} catch (Exception e) {
					log.error("Teardown for path " + pathId + " failed: " + e.getMessage());
					listener.notifyIntraDomainPathModification(interDomainPathId, pathId, PathLifecycleAction.TEARDOWN, OperationResult.FAILED);
				}
			} else throw new EntityNotFoundException("Domain " + domainId + " not configured.");
		} else throw new EntityNotFoundException("Intra domain path " + pathId + " not found.");
	}

}
