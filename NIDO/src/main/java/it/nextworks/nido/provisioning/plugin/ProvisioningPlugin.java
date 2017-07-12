
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.plugin;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.provisioning.ProvisioningPluginInterface;
import it.nextworks.nido.provisioning.topology.Domain;

public abstract class ProvisioningPlugin implements ProvisioningPluginInterface {

	protected RestTemplate restTemplate;
	
	private String controllerUrlSuffix;
	private String agentUrlSuffix;
	
	private ProvisioningPluginType pluginType;
	private String url;
	protected String domainId;
	
	public ProvisioningPlugin(ProvisioningPluginType pluginType, String domainId, String url, 
			String controllerUrlSuffix, String agentUrlSuffix) {
		this(pluginType, domainId, url, controllerUrlSuffix, agentUrlSuffix, new RestTemplate());
	}

	public ProvisioningPlugin(ProvisioningPluginType pluginType, String domainId, String url,
							  String controllerUrlSuffix, String agentUrlSuffix, RestTemplate restTemplate) {
		this.pluginType = pluginType;
		this.url = url;
		this.domainId = domainId;
		this.controllerUrlSuffix = controllerUrlSuffix;
		this.agentUrlSuffix = agentUrlSuffix;
		this.restTemplate = restTemplate;
	}

	/**
	 * @return the pluginType
	 */
	public ProvisioningPluginType getPluginType() {
		return pluginType;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @return the controllerUrlSuffix
	 */
	public String getControllerUrlSuffix() {
		return controllerUrlSuffix;
	}

	/**
	 * @return the agentUrlSuffix
	 */
	public String getAgentUrlSuffix() {
		return agentUrlSuffix;
	}
	
	
	public String getAgentUrl() {
		return (url + agentUrlSuffix);
	}
	
	public String getControllerUrl() {
		return (url + controllerUrlSuffix);
	}
	
	public Domain readTopology()
			throws GeneralFailureException {
		try {
			String agentUrl = getAgentUrl();
			HttpHeaders header = new HttpHeaders();
			header.add("Content-Type", "application/json");

			HttpEntity<Domain> entity = new HttpEntity<Domain>(null, header);
			ResponseEntity<Domain> httpResponse = restTemplate.exchange(agentUrl, HttpMethod.GET, entity, Domain.class);
			switch (httpResponse.getStatusCode()) {
			case OK:
				Domain result = httpResponse.getBody();
				return result;

			default:
				return null;
			}
		} catch (Exception e) {
			throw new GeneralFailureException("Error while reading topology: " + e.getMessage());
		}
	}
	

}
