
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
import it.nextworks.nido.path.ConnectionType;

/**
 * Created by Marco Capitani on 22/06/17.
 *
 * @author Marco Capitani(m.capitani AT nextworks.it)
 */

public class Connection {

    public Connection(){
        // Jackson constructor
    };

    public Connection(EndPoint sourceEndPoint,
                      EndPoint destinationEndPoint,
                      Recovery recovery,
                      TrafficProfile trafficProfile,
                      ConnectionType connectionType,
                      String destIp) {
        this.destinationEndPoint = destinationEndPoint;
        this.recovery = recovery;
        this.trafficProfile = trafficProfile;
        this.connectionType = connectionType;
        this.sourceEndPoint = sourceEndPoint;
        this.destIp = destIp;
    }

    @JsonProperty("Destination_end_point")
    public EndPoint destinationEndPoint;

    @JsonProperty("Recovery")
    public Recovery recovery;

    @JsonProperty("Traffic_profile")
    public TrafficProfile trafficProfile;

    @JsonProperty("Connection_type")
    public ConnectionType connectionType;

    @JsonProperty("Source_end_point")
    public EndPoint sourceEndPoint;

    @JsonProperty("Destination_IP")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String destIp;

    @Override
    public String toString() {
        return "\nConnection{\n\t" +
                "destinationEndPoint=" + destinationEndPoint.toString() +
                ",\n\t recovery=" + recovery.toString() +
                ",\n\t trafficProfile=" + trafficProfile.toString() +
                ",\n\t connectionType=" + connectionType.toString() +
                ",\n\t sourceEndPoint=" + sourceEndPoint.toString() +
                ",\n\t destIp='" + destIp + '\'' +
                "\n}\n";
    }
}