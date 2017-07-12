
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.elements;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

    /**
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class JuliusRequest {

    @JsonProperty("demands")
    public List<JuliusConnectionRequest> demands;

}