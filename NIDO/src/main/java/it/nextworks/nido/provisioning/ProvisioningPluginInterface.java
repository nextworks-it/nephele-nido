
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning;

import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.engine.PathNotificationListenerInterface;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.provisioning.topology.Domain;

public interface ProvisioningPluginInterface {

	public String setupIntraDomainPath(String interDomainPathId, IntraDomainPath path, PathNotificationListenerInterface listener) 
			throws EntityNotFoundException, GeneralFailureException;
	
	public void teardownIntraDomainPath(String interDomainPathId, String intraDomainPathId, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException;
	
	public Domain readTopology()
		throws GeneralFailureException;
	
}
