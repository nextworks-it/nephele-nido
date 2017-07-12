
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nextworks.nido.common.exceptions.EntityAlreadyExistingException;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;



@Service
public class TopologyDbWrapper {

	private static final Logger log = LoggerFactory.getLogger(TopologyDbWrapper.class);
	
	@Autowired
	private DomainsRepository domainsRepository;
	
	@Autowired
	private NodeRepository nodeRepository;
	
	public TopologyDbWrapper() { }
	
	public synchronized void addHostsToDomain(String domainId, List<Host> hosts) throws EntityNotFoundException {
		log.debug("Received request to add hosts in domain " + domainId);
		Domain domain = getDomain(domainId);
		domain.setHosts(hosts);
		domainsRepository.saveAndFlush(domain);
		log.debug("Hosts added in domain " + domainId + ". Updated domain: " + domain.toString());
	}
	
	public synchronized void addNodesToDomain(String domainId, List<Node> nodes) throws EntityNotFoundException {
		log.debug("Received request to add nodes in domain " + domainId);
		Domain domain = getDomain(domainId);
		List<Node> newNodes = new ArrayList<>();
		for (Node n : nodes) {
			Node targetNode = new Node(domain, n.getNodeId(), n.getLinks());
			newNodes.add(targetNode);
			//nodeRepository.saveAndFlush(targetNode);
			//log.debug("Node " + n.getNodeId() + " added in topology DB.");
			log.debug("Node " + n.getNodeId() + " added in list.");
		}
		domain.setNodes(newNodes);
		domainsRepository.saveAndFlush(domain);
//		List<Node> nodes1 = nodeRepository.findAll();
//		for (Node n : nodes1) log.debug(n.toString());
		Domain updatedDomain = domainsRepository.findByDomainId(domainId).get();
		log.debug("Nodes added in domain " + domainId + ". Updated domain: " + updatedDomain.toString());
	}
	
	public synchronized void addDomain(Domain domain) throws EntityAlreadyExistingException {
		log.debug("Received request to create new domain: " + domain.toString());
		String domainId = domain.getDomainId();
		if (domainsRepository.findByDomainId(domainId).isPresent()) {
			log.debug("Domain with ID " + domainId + " already present in DB. Cannot add another one.");
			throw new EntityAlreadyExistingException("Domain with ID " + domainId + " already present in DB. Cannot add another one.");
		}
		Domain domainTarget = new Domain(domain.getDomainType(), domainId, domain.getUrl(), domain.getType());
		domainsRepository.saveAndFlush(domainTarget);
		log.debug("Domain " + domainId + " added in topology DB.");
	}
	
	public synchronized void removeDomain(String domainId) throws EntityNotFoundException {
		log.debug("Received request to remove domain " + domainId + " from the topology DB.");
		Domain domain = getDomain(domainId);
		domainsRepository.delete(domain);
		log.info("Domain deleted from DB");
	}
	
	public List<Domain> getAllDomains() {
		log.debug("Received request to retrieve all the domains from the topology DB");
		List<Domain> domains = domainsRepository.findAll();
		return domains;
	}
	
	public List<Domain> getAllCoreDomains() {
		log.debug("Received request to retrieve all the core domains from the topology DB");
		List<Domain> domains = domainsRepository.findByType(DomainType.CORE);
//		for (Domain d : domains) {
//			log.debug(d.toString());
//			String domainId = d.getDomainId();
//			Domain d1 = domainsRepository.findByDomainId(domainId).get();
//			log.debug("Nodes size: " + d1.getNodes().size());
//			List<Node> nodes = d1.getNodes();
//			for (Node n : nodes) log.debug(n.toString());
//		}
		return domains;
	}
	
	public List<Domain> getAllAccessDomains() {
		log.debug("Received request to retrieve all the access domains from the topology DB");
		List<Domain> domains = domainsRepository.findByType(DomainType.ACCESS);
		return domains;
	}
	
	public Domain getDomain(String domainId) throws EntityNotFoundException {
		log.debug("Received request to retrieve domain " + domainId + " from the topology DB.");
		Optional<Domain> domainOpt = domainsRepository.findByDomainId(domainId);
		if (domainOpt.isPresent()) {
			log.debug("Domain found");
			return domainOpt.get();
		} else {
			log.error("Domain not found in topology DB.");
			throw new EntityNotFoundException("Domain " + domainId + " not found in topology DB.");
		}
	}
	
	public Node getNode(String nodeId) throws EntityNotFoundException {
		log.debug("Received request to retrieve node " + nodeId + " from the topology DB.");
		Optional<Node> nodeOpt = nodeRepository.findByNodeId(nodeId);
		if (nodeOpt.isPresent()) {
			log.debug("Node found");
			return nodeOpt.get();
		} else {
			log.error("Node not found in topology DB.");
			throw new EntityNotFoundException("Node " + nodeId + " not found in topology DB.");
		}
	}

}
