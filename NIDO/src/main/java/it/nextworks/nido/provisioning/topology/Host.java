
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.topology;

import javax.persistence.Embeddable;

@Embeddable
public class Host {

	private String nodeId;
	private String portId;
	
	public Host() {	}
	
	public Host(String nodeId, String portId) {
		this.nodeId = nodeId;
		this.portId = portId;
	}

	/**
	 * @return the nodeId
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @return the portId
	 */
	public String getPortId() {
		return portId;
	}
	
	

}
