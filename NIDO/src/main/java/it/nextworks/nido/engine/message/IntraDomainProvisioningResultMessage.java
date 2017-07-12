
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.engine.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.enums.PathLifecycleAction;

public class IntraDomainProvisioningResultMessage extends EngineMessage {

	@JsonProperty("intraDomainPathId")
	private String intraDomainPathId;
	
	@JsonProperty("result")
	private OperationResult result;
	
	@JsonProperty("action")
	private PathLifecycleAction pathLifecycleAction;
	
	@JsonCreator
	public IntraDomainProvisioningResultMessage(@JsonProperty("interDomainPathId") String interDomainPathId,
			@JsonProperty("intraDomainPathId") String intraDomainPathId,
			@JsonProperty("result") OperationResult result,
			@JsonProperty("action") PathLifecycleAction pathLifecycleAction) {
		this.type = QueueMessageType.INTRA_DOMAIN_PROVISIONING_RESULT;
		this.intraDomainPathId = intraDomainPathId;
		this.interDomainPathId = interDomainPathId;
		this.result = result;
		this.pathLifecycleAction = pathLifecycleAction;
	}

	/**
	 * @return the intraDomainPathId
	 */
	public String getIntraDomainPathId() {
		return intraDomainPathId;
	}

	/**
	 * @return the result
	 */
	public OperationResult getResult() {
		return result;
	}

	/**
	 * @return the pathLifecycleAction
	 */
	public PathLifecycleAction getPathLifecycleAction() {
		return pathLifecycleAction;
	}
	
	

}
