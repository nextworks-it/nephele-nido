
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

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;

@Entity
public class Node {

	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

	@JsonIgnore
	@ManyToOne
	private Domain domain;
	
	private String nodeId;
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@ElementCollection(fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@Cascade(org.hibernate.annotations.CascadeType.ALL)
	private List<Link> links = new ArrayList<>();
	
	public Node() {
		// JPA only
	}
	
	public Node(Domain domain,
			String nodeId,
			List<Link> links) {
		this.domain = domain;
		this.nodeId = nodeId;
		if (links != null) this.links = links;
	}

	/**
	 * @return the domain
	 */
	public Domain getDomain() {
		return domain;
	}

	/**
	 * @return the nodeId
	 */
	@ApiModelProperty(notes="The ID of the network edge node.")
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @return the links
	 */
	@ApiModelProperty(notes="The list of inter-domain links that interconnect the edge node to a neighbour domain.")
	public List<Link> getLinks() {
		return links;
	}
	
	@JsonIgnore
	public Link getLinkWithPeerNode(String peerNode) throws EntityNotFoundException {
		for (Link l : links) {
			if (l.getPeerNodeId().equals(peerNode)) return l;
		}
		throw new EntityNotFoundException();
	}
	
	@JsonIgnore
	public String getDomainId() {
		return domain.getDomainId();
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Node: [ Node ID: " + nodeId;
		if ((links != null) && (!(links.isEmpty()))) {
			s += "\n Links: ";
			for (Link l : links) {
				s += "\n " + l.toString();
			}
		}
		s += "\n ]";
		return s;
	}

}
