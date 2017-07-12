
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
     * Created by json2java on 22/06/17.
     * json2java author: Marco Capitani (m.capitani AT nextworks DOT it)
     */

public class JuliusConnectionRequest {

    public JuliusConnectionRequest(){

    }

    public JuliusConnectionRequest(int id, int src, int dst, int bidirectional, int bandwidth) {
        this.src = src;
        this.id = id;
        this.bidirectional = bidirectional;
        this.dst = dst;
        this.bandwidth = bandwidth;
    }

    @JsonProperty("src")
    public Integer src;

    @JsonProperty("id")
    public Integer id;

    @JsonProperty("bidirectional")
    public Integer bidirectional;

    @JsonProperty("dst")
    public Integer dst;

    @JsonProperty("bandwidth")
    public Integer bandwidth;

}