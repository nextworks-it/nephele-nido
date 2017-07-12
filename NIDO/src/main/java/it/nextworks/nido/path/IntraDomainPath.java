
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.path;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;

@Entity
@DiscriminatorValue("INTRA")
public class IntraDomainPath extends Path {

	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;
	
	@JsonIgnore
	@ManyToOne
	private InterDomainPath idp;
	
	//Note: at the multi-domain orchestrator level we do not need any detail of the internal path
	
	private String domainId;
	private String internalId;
	
	public IntraDomainPath() {
		//JPA only
	}
	
	public IntraDomainPath(InterDomainPath idp, PathEdge sourceEndPoint, PathEdge destinationEndPoint,
			ConnectionType connectionType, TrafficClassifier trafficClassifier, TrafficProfile trafficProfile, PathStatus pathStatus,
			String domainId, String internalId) {
		super(PathType.INTRA_DOMAIN, sourceEndPoint, destinationEndPoint, connectionType, trafficClassifier, trafficProfile, pathStatus);
		this.idp = idp;
		this.domainId = domainId;
		this.internalId = internalId;
	}
	
	@JsonIgnore
	public String toString() {
		String s = super.toString();
		if (domainId != null) s+= "Domain ID: " + domainId + "\n";
		if (internalId != null) s+= "Internal ID: " + internalId + "\n";
		return s;
	}

	/**
	 * @return the domainId
	 */
	@ApiModelProperty(notes="The ID of the domain this path is referred to.")
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @return the internalId
	 */
	@ApiModelProperty(notes="The ID of the intra-domain path")
	public String getInternalId() {
		return internalId;
	}

	/**
	 * @param internalId the internalId to set
	 */
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	

}
