
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
import it.nextworks.nido.path.InterDomainPath;

public class PathComputationResultMessage extends EngineMessage {

	@JsonProperty("path")
	private InterDomainPath interDomainPath;
	
	@JsonProperty("result")
	private OperationResult result;
	
	@JsonCreator
	public PathComputationResultMessage(@JsonProperty("interDomainPathId") String interDomainPathId,
			@JsonProperty("path") InterDomainPath interDomainPath,
			@JsonProperty("result") OperationResult result) {
		this.type = QueueMessageType.PATH_COMPUTATION_RESULT;
		this.interDomainPathId = interDomainPathId;
		this.interDomainPath = interDomainPath;
		this.result = result;
	}

	/**
	 * @return the interDomainPath
	 */
	public InterDomainPath getInterDomainPath() {
		return interDomainPath;
	}

	/**
	 * @return the result
	 */
	public OperationResult getResult() {
		return result;
	}
	
	

	
	
}
