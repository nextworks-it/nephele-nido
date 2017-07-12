/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.engine.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "msgType")
@JsonSubTypes({
	@Type(value = InterDomainPathSetupMessage.class, 	name = "SETUP_REQUEST"),
	@Type(value = InterDomainPathTearDownMessage.class, 	name = "TEARDOWN_REQUEST"),
	@Type(value = PathComputationResultMessage.class, 	name = "NOTIFY_COMPUTATION"),
	@Type(value = IntraDomainProvisioningResultMessage.class, 	name = "NOTIFY_PROVISIONING"),
})
public abstract class EngineMessage {
	
	@JsonProperty("type")
	QueueMessageType type;
	
	@JsonProperty("interDomainPathId")
	String interDomainPathId;

	/**
	 * @return the type
	 */
	public QueueMessageType getType() {
		return type;
	}

	/**
	 * @return the interDomainPathId
	 */
	public String getInterDomainPathId() {
		return interDomainPathId;
	}

	

}
