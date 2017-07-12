
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.enums.PathLifecycleAction;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.engine.PathNotificationListenerInterface;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.provisioning.topology.Domain;
import it.nextworks.nido.provisioning.topology.DomainType;
import it.nextworks.nido.provisioning.topology.Host;
import it.nextworks.nido.provisioning.topology.Link;
import it.nextworks.nido.provisioning.topology.Node;

public class DummyProvisioningPlugin extends ProvisioningPlugin {
	
	private static final Logger log = LoggerFactory.getLogger(DummyProvisioningPlugin.class);

	public DummyProvisioningPlugin(String domainId, String url) {
		super(ProvisioningPluginType.PLUGIN_DUMMY, domainId, url, ":8181/", ":8888/topology");
	}

	@Override
	public String setupIntraDomainPath(String interDomainPathId, IntraDomainPath path, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		String intraDomainPathId = UUID.randomUUID().toString();
		listener.notifyIntraDomainPathModification(interDomainPathId, intraDomainPathId, PathLifecycleAction.SETUP, OperationResult.COMPLETED);
		return intraDomainPathId;
	}

	@Override
	public void teardownIntraDomainPath(String interDomainPathId, String intraDomainPathId, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		listener.notifyIntraDomainPathModification(interDomainPathId, intraDomainPathId, PathLifecycleAction.TEARDOWN, OperationResult.COMPLETED);
	}
	
	@Override
	public Domain readTopology()
			throws GeneralFailureException {
		if (domainId.equals("DCN_01")) {
			return getDcn01();
		} else if (domainId.equals("DCN_02")) {
			return getDcn02();
		} else if (domainId.equals("DCN_03")) {
			return getDcn03();
		} else if (domainId.equals("InterDC_01")) {
			return getInterDcNet();
		} else {
			log.error("Dummy domain not configured. Impossible to return topology.");
			throw new GeneralFailureException("Dummy domain not configured. Impossible to return topology.");
		}
	}
	
	private List<Host> buildHosts(int startPod, List<String> invalidHosts) {
		List<Host> hosts = new ArrayList<>();
		for (int i=0; i<10; i++) {
			int currentPod = startPod + i;
			for (int j=1; j<11; j++) {
				int currentToR = j;
				String s = buildTorId(currentPod, currentToR);
				if (!(isInvalidHost(s, invalidHosts))) {
					hosts.add(new Host(s, "1"));
				}
			}
		}
		return hosts;
	}
	
	private boolean isInvalidHost(String id, List<String> invalidHosts) {
		for (String s : invalidHosts) {
			if (id.equals(s)) return true;
		}
		return false;
	}
	
	private List<Node> buildNodesForEdgeDomain(int startExitPod, int startIngressPeer) {
		List<Node> nodes = new ArrayList<>();
		for (int i=0; i<3; i++) {
			int currentPod = startExitPod + i;
			int currentPeer = startIngressPeer + i;
			String s = buildTorId(currentPod, 10);
			List<Link> links = new ArrayList<>();
			Link l = new Link("InterDC_01", "4", Integer.toString(currentPeer), "1");
			links.add(l);
			nodes.add(new Node(null, s, links));
		}
		return nodes;
	}
	
	private Domain getDcn01() {
		Domain dcn = new Domain(ProvisioningPluginType.PLUGIN_DUMMY, "DCN_01", "http://127.0.0.1", DomainType.ACCESS);
		
		List<String> invalidHosts = new ArrayList<>();
		invalidHosts.add("117010");
		invalidHosts.add("118010");
		invalidHosts.add("119010");
		List<Host> hosts = buildHosts(10, invalidHosts);
		dcn.setHosts(hosts);
		
		List<Node> nodes = buildNodesForEdgeDomain(17, 1);
		dcn.setNodes(nodes);
		
		log.debug("Built domain: " + dcn.toString());
		return dcn;
	}
	
	private Domain getDcn02() {
		Domain dcn = new Domain(ProvisioningPluginType.PLUGIN_DUMMY, "DCN_02", "http://127.0.0.1", DomainType.ACCESS);
		
		List<String> invalidHosts = new ArrayList<>();
		invalidHosts.add("127010");
		invalidHosts.add("128010");
		invalidHosts.add("129010");
		List<Host> hosts = buildHosts(20, invalidHosts);
		dcn.setHosts(hosts);
		
		List<Node> nodes = buildNodesForEdgeDomain(27, 4);
		dcn.setNodes(nodes);
		
		log.debug("Built domain: " + dcn.toString());
		return dcn;
	}
	
	private Domain getDcn03() {
		Domain dcn = new Domain(ProvisioningPluginType.PLUGIN_DUMMY, "DCN_03", "http://127.0.0.1", DomainType.ACCESS);
		
		List<String> invalidHosts = new ArrayList<>();
		invalidHosts.add("137010");
		invalidHosts.add("138010");
		invalidHosts.add("139010");
		List<Host> hosts = buildHosts(30, invalidHosts);
		dcn.setHosts(hosts);
		
		List<Node> nodes = buildNodesForEdgeDomain(37, 7);
		dcn.setNodes(nodes);
		
		log.debug("Built domain: " + dcn.toString());
		return dcn;
	}
	
	private Node buildInterDcNode(String localNodeId, String peerDomainId, String peerNodeId) {
		List<Link> links = new ArrayList<>();
		Link l = new Link(peerDomainId, "1", peerNodeId, "4");
		links.add(l);
		Node node = new Node(null, localNodeId, links);
		return node;
	}
	
	private Domain getInterDcNet() {
		Domain dcn = new Domain(ProvisioningPluginType.PLUGIN_DUMMY, "InterDC_01", "http://127.0.0.1", DomainType.CORE);
		List<Node> nodes = new ArrayList<>();
		nodes.add(buildInterDcNode("1", "DCN_01", "117010"));
		nodes.add(buildInterDcNode("2", "DCN_01", "118010"));
		nodes.add(buildInterDcNode("3", "DCN_01", "119010"));
		nodes.add(buildInterDcNode("4", "DCN_02", "127010"));
		nodes.add(buildInterDcNode("5", "DCN_02", "128010"));
		nodes.add(buildInterDcNode("6", "DCN_02", "129010"));
		nodes.add(buildInterDcNode("7", "DCN_03", "137010"));
		nodes.add(buildInterDcNode("8", "DCN_03", "138010"));
		nodes.add(buildInterDcNode("9", "DCN_03", "139010"));
		dcn.setNodes(nodes);
		log.debug("Built domain: " + dcn.toString());
		return dcn;
	}
	
	private String buildTorId(int pod, int tor) {
		String s = "1" + Integer.toString(pod);
		if (tor<10) s +="00";
		else s +="0";
		s += Integer.toString(tor);
		return  s;
	}
}
