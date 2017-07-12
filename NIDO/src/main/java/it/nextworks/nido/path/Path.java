
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.path;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;

@Entity
@Inheritance
@DiscriminatorColumn(name="TYPE_PATH")
public class Path {
	
	@Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

	private PathType pathType;
	
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="domainId",column=@Column(name="srcDomainId")),
		@AttributeOverride(name="nodeId",column=@Column(name="srcNodeId")),
		@AttributeOverride(name="portId",column=@Column(name="srcPortId")),
	})
	private PathEdge sourceEndPoint;
	
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="domainId",column=@Column(name="dstDomainId")),
		@AttributeOverride(name="nodeId",column=@Column(name="dstNodeId")),
		@AttributeOverride(name="portId",column=@Column(name="dstPortId")),
	})
	private PathEdge destinationEndPoint;
	
	private ConnectionType connectionType;
	
	@Embedded
	private TrafficClassifier trafficClassifier;
	
	@Embedded
	private TrafficProfile trafficProfile;
	
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private PathStatus pathStatus;
	
	public Path() {
		//JPA only
	}
	
	public Path(PathType pathType,
			PathEdge sourceEndPoint,
			PathEdge destinationEndPoint,
			ConnectionType connectionType,
			TrafficClassifier trafficClassifier,
			TrafficProfile trafficProfile,
			PathStatus pathStatus) {
		this.pathType = pathType;
		this.sourceEndPoint = sourceEndPoint;
		this.destinationEndPoint = destinationEndPoint;
		this.connectionType = connectionType;
		this.trafficClassifier = trafficClassifier;
		this.trafficProfile = trafficProfile;
		this.pathStatus = pathStatus;
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Path description: \n Path type: " + pathType.toString() + "\n";
		if (sourceEndPoint != null) s += "Source: " + sourceEndPoint.toString() + "\n";
		if (destinationEndPoint != null) s += "Destination: " + destinationEndPoint.toString() + "\n";
		s += "Connection type: " + connectionType + "\n";
		if (trafficClassifier != null) s += trafficClassifier.toString() + "\n";
		if (trafficProfile != null) s += trafficProfile.toString() + "\n";
		s += "Path status: " + pathStatus.toString() + "\n";
		return s;
	}

	/**
	 * @return the pathType
	 */
	@ApiModelProperty(notes="The type of the path.")
	public PathType getPathType() {
		return pathType;
	}

	/**
	 * @return the sourceEndPoint
	 */
	@ApiModelProperty(notes="The source of the path.")
	public PathEdge getSourceEndPoint() {
		return sourceEndPoint;
	}

	/**
	 * @return the destinationEndPoint
	 */
	@ApiModelProperty(notes="The destination of the path.")
	public PathEdge getDestinationEndPoint() {
		return destinationEndPoint;
	}

	/**
	 * @return the connectionType
	 */
	@ApiModelProperty(notes="The connection type of the path.")
	public ConnectionType getConnectionType() {
		return connectionType;
	}

	/**
	 * @return the trafficClassifier
	 */
	@ApiModelProperty(notes="The classifier of the traffic that is carried through the path.")
	public TrafficClassifier getTrafficClassifier() {
		return trafficClassifier;
	}
	
	

	/**
	 * @return the trafficProfile
	 */
	@ApiModelProperty(notes="The QoS parameters of the traffic profile associated to the path.")
	public TrafficProfile getTrafficProfile() {
		return trafficProfile;
	}

	/**
	 * @return the pathStatus
	 */
	@ApiModelProperty(notes="The current status of the path.")
	public PathStatus getPathStatus() {
		return pathStatus;
	}

	/**
	 * @param pathStatus the pathStatus to set
	 */
	public void setPathStatus(PathStatus pathStatus) {
		this.pathStatus = pathStatus;
	}
	
	

}
