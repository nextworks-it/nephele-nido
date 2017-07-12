
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

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;
import it.nextworks.nido.provisioning.plugin.ProvisioningPluginType;

@Entity
public class Domain {

	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;
	
	private ProvisioningPluginType domainType;
	private DomainType type;
	private String domainId;
	private String url;
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@ElementCollection(fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@Cascade(org.hibernate.annotations.CascadeType.ALL)
	private List<Host> hosts = new ArrayList<>();
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@OneToMany(mappedBy = "domain", fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Node> nodes = new ArrayList<>();
	
	public Domain() {
		// JPA only
	}
	
	/**
	 * Constructor
	 * 
	 * @param domainType type of domain
	 * @param domainId ID of the domain
	 * @param url url where the domain controller can be reached
	 */
	public Domain(ProvisioningPluginType domainType, String domainId, String url, DomainType type) {
		this.domainId = domainId;
		this.domainType = domainType;
		this.url = url;
		this.type = type;
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Domain: [Domain ID: " + domainId + " - Plugin type: " + domainType + " - Domain type: " + type + " - URL: " + url;
		if ((hosts != null) && (!(hosts.isEmpty()))) {
			s += "\n Hosts: ";
			for (Host h : hosts) {
				s += "\n NodeId: " + h.getNodeId() + " - PortId: " + h.getPortId();
			}
		}
		if ((nodes != null) && (!(nodes.isEmpty()))) {
			s += "\n Nodes: ";
			for (Node n : nodes) {
				s += "\n " + n.toString();
			}
		}
		s += "]";
		return s;
	}

	/**
	 * @return the domainType
	 */
	@ApiModelProperty(notes="Type of the plugin that interacts with the network domain.")
	public ProvisioningPluginType getDomainType() {
		return domainType;
	}

	/**
	 * @return the domainId
	 */
	@ApiModelProperty(notes="ID of the network domain.")
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @return the url
	 */
	@ApiModelProperty(notes="URL to connect to the controller managing the network domain.")
	public String getUrl() {
		return url;
	}

	/**
	 * @return the hosts
	 */
	@ApiModelProperty(notes="The list of hosts attached to the network domain.")
	public List<Host> getHosts() {
		return hosts;
	}

	/**
	 * @param hosts the hosts to set
	 */
	public void setHosts(List<Host> hosts) {
		this.hosts = hosts;
	}

	/**
	 * @return the nodes
	 */
	@ApiModelProperty(notes="The list of the edge nodes of the network domains")
	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * @param nodes the nodes to set
	 */
	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	/**
	 * @return the type
	 */
	@ApiModelProperty(notes="The type of the network domain")
	public DomainType getType() {
		return type;
	}
	
	
	

}
