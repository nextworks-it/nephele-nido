
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
public class TrafficClassifier {

	//Very simple one. If needed it should be extended with different type of traffic classifier
	private String srcIpAddress;
	private String dstIpAddress;
	private String dstEthAddress;
	private int vlanId;
	
	public TrafficClassifier() {
		//JPA only
	}
	
	public TrafficClassifier(String srcIpAddress,
			String dstIpAddress,
			String dstEthAddress,
			int vlanId) {
		this.srcIpAddress = srcIpAddress;
		this.dstEthAddress = dstEthAddress;
		this.dstIpAddress = dstIpAddress;
		this.vlanId = vlanId;
	}
	
	@JsonIgnore
	public String toString() {
		String s = "Traffic classifier: [Src IP: " + srcIpAddress + " - Dst IP: " + dstIpAddress + " - Dst ETH: " + dstEthAddress + " - VLAN ID: " + vlanId + "]";
		return s;
	}


	/**
	 * @return the srcIpAddress
	 */
	@ApiModelProperty(notes="The source IP address of the traffic.")
	public String getSrcIpAddress() {
		return srcIpAddress;
	}


	/**
	 * @return the dstIpAddress
	 */
	@ApiModelProperty(notes="The destination IP address of the traffic.")
	public String getDstIpAddress() {
		return dstIpAddress;
	}


	/**
	 * @return the dstEthAddress
	 */
	@ApiModelProperty(notes="The destination MAC address of the traffic.")
	public String getDstEthAddress() {
		return dstEthAddress;
	}


	/**
	 * @return the vlanId
	 */
	@ApiModelProperty(notes="The VLAN ID the traffic is tagged with.")
	public int getVlanId() {
		return vlanId;
	}
	
	

}
