
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.pathcomputation.algorithm;

import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.pathcomputation.PathComputationListenerInterface;

public interface MdComputationAlgorithmInterface {

	public void computeInterDomainPath(InterDomainPath path, PathComputationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException;
	
}
