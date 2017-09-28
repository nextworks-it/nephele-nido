
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.elements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
     * Created by Marco Capitani on 22/06/17.
     *
     * @author Marco Capitani(m.capitani AT nextworks.it)
     */

public class Service {

    @JsonProperty("connections")
    public List<Connection> connections = new ArrayList<>();

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ServiceStatus status;

    public void addConnection(Connection connection) {
        connections.add(connection);
    }

    @Override
    public String toString() {
        return "Service{" +
                "connections=" + connections.toString() +
                '}';
    }
}