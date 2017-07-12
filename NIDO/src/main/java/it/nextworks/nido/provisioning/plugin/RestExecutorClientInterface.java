
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.plugin;

import java.util.UUID;

/**
 * Created by Marco Capitani on 06/07/17.
 *
 * @author Marco Capitani (m.capitani AT nextworks.it)
 */
public interface RestExecutorClientInterface {
    <T> void notifySuccess(String intraDomainPathId, T body, Class<T> responseType);

    void notifyFailure(String intraDomainPathId, String message);

    void notifyFailure(String intraDomainPathId, Throwable exc);

    void notifyFailure(String intraDomainPathId, String message, Throwable exc);
}
