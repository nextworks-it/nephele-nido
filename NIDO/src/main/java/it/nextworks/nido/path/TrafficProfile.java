
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
public class TrafficProfile {

	private int bandwidth;
	
	public TrafficProfile() { }
	
	
	public TrafficProfile(int bandwidth) {
		this.bandwidth = bandwidth;
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Traffic profile: [Bw: " + bandwidth + "]";
		return s;
	}

	/**
	 * @return the bandwidth
	 */
	@ApiModelProperty(notes="The maximum requested bandwidth for the traffic on the path.")
	public int getBandwidth() {
		return bandwidth;
	}
	
	

}
