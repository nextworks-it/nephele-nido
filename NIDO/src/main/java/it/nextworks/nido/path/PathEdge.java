
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.path;

import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;

@Embeddable
public class PathEdge {

	//Note: it may be extended to cover specific type of end points
	private String domainId;
	private String nodeId;
	private String portId;
	
	public PathEdge() {
		//JPA only
	}
	
	public PathEdge(String domainId, String nodeId, String portId) {
		this.domainId = domainId;
		this.nodeId = nodeId;
		this.portId = portId;
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Path Edge: [Domain ID: " + domainId + " - Node ID: " + nodeId + " - Port ID: " + portId + "]";
		return s;
	}

	/**
	 * @return the domainId
	 */
	@ApiModelProperty(notes="The ID of the domain where the edge node is placed.")
	public String getDomainId() {
		return domainId;
	}



	/**
	 * @return the nodeId
	 */
	@ApiModelProperty(notes="The ID of the edge node.")
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @return the portId
	 */
	@ApiModelProperty(notes="The ID of the ingress (egress) port on the source (destination) edge node.")
	public String getPortId() {
		return portId;
	}
	
	

}
