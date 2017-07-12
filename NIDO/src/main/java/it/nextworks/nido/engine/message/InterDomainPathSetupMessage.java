
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

import it.nextworks.nido.path.InterDomainPath;

public class InterDomainPathSetupMessage extends EngineMessage {

	@JsonProperty("path")
	private InterDomainPath interDomainPath;
	
	@JsonCreator
	public InterDomainPathSetupMessage(@JsonProperty("interDomainPathId") String interDomainPathId,
			@JsonProperty("path") InterDomainPath interDomainPath) {	
		this.type = QueueMessageType.SETUP_PATH_REQUEST;
		this.interDomainPathId = interDomainPathId;
		this.interDomainPath = interDomainPath;
	}

	/**
	 * @return the interDomainPath
	 */
	public InterDomainPath getInterDomainPath() {
		return interDomainPath;
	}
	
	

}
