
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.topology;

import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;

@Embeddable
public class Link {

	private String peerDomainId;
	private String localPortId;
	private String peerNodeId;
	private String peerPortId;
	
	public Link() {
		// JPA only
	}
	
	public Link(String peerDomainId,
			String localPortId,
			String peerNodeId,
			String peerPortId) {
		this.peerDomainId = peerDomainId;
		this.localPortId = localPortId;
		this.peerNodeId = peerNodeId;
		this.peerPortId = peerPortId;
	}

	/**
	 * @return the peerDomainId
	 */
	@ApiModelProperty(notes="The ID of the neighbour domain this link is connected to.")
	public String getPeerDomainId() {
		return peerDomainId;
	}

	/**
	 * @return the localPortId
	 */
	@ApiModelProperty(notes="The ID of the local port this link is connected to.")
	public String getLocalPortId() {
		return localPortId;
	}

	/**
	 * @return the peerNodeId
	 */
	@ApiModelProperty(notes="The ID of the edge node on the neighbour domain this link is connected to.")
	public String getPeerNodeId() {
		return peerNodeId;
	}

	/**
	 * @return the peerPortId
	 */
	@ApiModelProperty(notes="The ID of the port of the edge node on the neighbour domain this link is connected to.")
	public String getPeerPortId() {
		return peerPortId;
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Link: [Local port ID: " + localPortId + " - Peer domain ID: " + peerDomainId + " - Peer node ID: " + peerNodeId + " - Peer port ID: " + peerPortId + " ]";
		return s;
	}

}
