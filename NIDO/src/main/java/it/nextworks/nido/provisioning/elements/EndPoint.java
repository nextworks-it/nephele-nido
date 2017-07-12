
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Marco Capitani on 22/06/17.
 *
 * @author Marco Capitani(m.capitani AT nextworks.it)
 */

public class EndPoint {

    public EndPoint(){
        // Jackson constructor
    }

    public EndPoint(Integer podId, Integer toRId, Integer zoneId) {
        this.podId = podId;
        this.toRId = toRId;
        this.zoneId = zoneId;
    }

    @JsonProperty("Pod_ID")
    public Integer podId;

    @JsonProperty("ToR_ID")
    public Integer toRId;

    @JsonProperty("Zone_ID")
    public Integer zoneId;

}