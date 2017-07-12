
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.path;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@DiscriminatorValue("INTER")
@ApiModel
public class InterDomainPath extends Path {

	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;
	
	
	private String interDomainPathId;
	private String name;
	
	@OneToMany(mappedBy = "idp", cascade=CascadeType.ALL)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<IntraDomainPath> intraDomainPaths = new ArrayList<>();
	
	public InterDomainPath() {
		//JPA only
	}
	
	public InterDomainPath(String interDomainPathId, String name, PathEdge sourceEndPoint, PathEdge destinationEndPoint,
			ConnectionType connectionType, TrafficClassifier trafficClassifier, TrafficProfile trafficProfile, PathStatus pathStatus) {
		super(PathType.INTER_DOMAIN, sourceEndPoint, destinationEndPoint, connectionType, trafficClassifier, trafficProfile, pathStatus);
		this.interDomainPathId = interDomainPathId;
		this.name = name;
	}
	
	@JsonIgnore
	public String toString() {
		String s = super.toString();
		if (interDomainPathId != null) s+= "Inter domain path ID: " + interDomainPathId + "\n";
		if (name != null) s+= "Inter domain path name: " + name + "\n";
		s+="Intra-domain paths: \n";
		for (IntraDomainPath internal : intraDomainPaths) {
			s+="============== \n" + internal.toString() + "============== \n"; 
		}
		return s;
	}

	/**
	 * @return the intraDomainPaths
	 */
	@ApiModelProperty(value = "The list of the intra-domain paths belonging to the inter-domain path")
	public List<IntraDomainPath> getIntraDomainPaths() {
		return intraDomainPaths;
	}

	/**
	 * @return the interDomainPathId
	 */
	@ApiModelProperty(required = true, value = "The ID of the inter-domain path")
	public String getInterDomainPathId() {
		return interDomainPathId;
	}

	/**
	 * @return the name
	 */
	@ApiModelProperty(required = true, value ="The name of the inter-domain path")
	public String getName() {
		return name;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param intraDomainPaths the intraDomainPaths to set
	 */
	public void setIntraDomainPaths(List<IntraDomainPath> intraDomainPaths) {
		this.intraDomainPaths = intraDomainPaths;
	}
	
	

}
