
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.plugin;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.enums.PathLifecycleAction;
import it.nextworks.nido.engine.PathNotificationListenerInterface;
import it.nextworks.nido.path.ConnectionType;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.PathEdge;
import it.nextworks.nido.path.PathStatus;
import it.nextworks.nido.path.TrafficClassifier;
import it.nextworks.nido.path.TrafficProfile;
import it.nextworks.nido.provisioning.elements.Response;
import it.nextworks.nido.provisioning.elements.Service;
import it.nextworks.nido.provisioning.elements.ServiceStatus;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Created by Marco Capitani on 07/07/17.
 *
 * @author Marco Capitani (m.capitani AT nextworks.it)
 */
public class OceaniaPluginTest {

    private RestExecutorInterface executor = mock(RestExecutorInterface.class);
    private RestTemplate template = mock(RestTemplate.class);
    private PathNotificationListenerInterface listener = mock(PathNotificationListenerInterface.class);

    private OceaniaPlugin plugin = new OceaniaPlugin("domain", "http://127.0.0.1", executor, template);

    @Test
    public void setupIntraDomainPath() throws Exception {
        // Setup response to the path request
        when(template.exchange(
                eq("http://127.0.0.1:8089/affinity/connection"),
                eq(HttpMethod.POST),
                any(),
                eq(Response.class))
        ).thenReturn(makeResponse(ServiceStatus.SCHEDULED, "test"));

        //Setup response to status check request
        when(executor.schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("test")
        )).thenReturn("test");

        PathEdge src = new PathEdge("domain", "101001", "1");
        PathEdge dst = new PathEdge("domain", "102001", "1");

        TrafficClassifier classifier = new TrafficClassifier(
                "10.1.1.1",
                "10.2.1.1",
                "00:04:00:02:01:01",
                1
        );

        TrafficProfile profile = new TrafficProfile(1);

        InterDomainPath interDomainPath = new InterDomainPath(
                "inter",
                "inter",
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.SCHEDULED
        );

        IntraDomainPath intraPath = new IntraDomainPath(
                interDomainPath,
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.REQUESTED,
                "domain",
                null
        );

        plugin.setupIntraDomainPath("inter", intraPath, listener);
        verify(executor).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("domain_test")
        );
        plugin.notifySuccess("domain_test", makeService(ServiceStatus.ACTIVE).getBody(), Service.class);
        verify(listener).notifyIntraDomainPathModification(
                eq("inter"),
                eq("domain_test"),
                eq(PathLifecycleAction.SETUP),
                eq(OperationResult.COMPLETED)
        );
    }

    @Test
    public void setupIntraDomainPath_LongWait() throws Exception {
        // Setup response to the path request
        when(template.exchange(
                eq("http://127.0.0.1:8089/affinity/connection"),
                eq(HttpMethod.POST),
                any(),
                eq(Response.class))
        ).thenReturn(makeResponse(ServiceStatus.SCHEDULED, "test"));

        //Setup response to status check request
        when(executor.schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("test")
        )).thenReturn("test");

        PathEdge src = new PathEdge("domain", "101001", "1");
        PathEdge dst = new PathEdge("domain", "102001", "1");

        TrafficClassifier classifier = new TrafficClassifier(
                "10.1.1.1",
                "10.2.1.1",
                "00:04:00:02:01:01",
                1
        );

        TrafficProfile profile = new TrafficProfile(1);

        InterDomainPath interDomainPath = new InterDomainPath(
                "inter",
                "inter",
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.SCHEDULED
        );

        IntraDomainPath intraPath = new IntraDomainPath(
                interDomainPath,
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.REQUESTED,
                "domain",
                null
        );

        plugin.setupIntraDomainPath("inter", intraPath, listener);
        verify(executor).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("domain_test")
        );
        plugin.notifySuccess("domain_test",
                makeService(ServiceStatus.ESTABLISHING).getBody(), Service.class);
        verify(executor, times(2)).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("domain_test")
        );
        plugin.notifySuccess("domain_test",
                makeService(ServiceStatus.ACTIVE).getBody(), Service.class);
        verify(listener).notifyIntraDomainPathModification(
                eq("inter"),
                eq("domain_test"),
                eq(PathLifecycleAction.SETUP),
                eq(OperationResult.COMPLETED)
        );
    }

    @Test
    public void setupIntraDomainPath_Failure() throws Exception {
        // Setup response to the path request
        when(template.exchange(
                eq("http://127.0.0.1:8089/affinity/connection"),
                eq(HttpMethod.POST),
                any(),
                eq(Response.class))
        ).thenReturn(makeResponse(ServiceStatus.SCHEDULED, "foo"));

        //Setup response to status check request
        when(executor.schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/foo"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("foo")
        )).thenReturn("foo");

        PathEdge src = new PathEdge("domain", "101001", "1");
        PathEdge dst = new PathEdge("domain", "102001", "1");

        TrafficClassifier classifier = new TrafficClassifier(
                "10.1.1.1",
                "10.2.1.1",
                "00:04:00:02:01:01",
                1
        );

        TrafficProfile profile = new TrafficProfile(1);

        InterDomainPath interDomainPath = new InterDomainPath(
                "inter",
                "inter",
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.SCHEDULED
        );

        IntraDomainPath intraPath = new IntraDomainPath(
                interDomainPath,
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.REQUESTED,
                "domain",
                null
        );

        plugin.setupIntraDomainPath("inter", intraPath, listener);
        verify(executor).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/foo"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("domain_foo")
        );
        plugin.notifySuccess("domain_foo", makeService(ServiceStatus.DELETED).getBody(), Service.class);
        verify(listener).notifyIntraDomainPathModification(
                eq("inter"),
                eq("domain_foo"),
                eq(PathLifecycleAction.SETUP),
                eq(OperationResult.FAILED)
        );
    }

    @Test
    public void teardownIntraDomainPathFailure() throws Exception {
        setupIntraDomainPath();
        // Setup response to the path request
        when(template.exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.DELETE),
                any(),
                eq(Object.class))
        ).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        //Setup response to status check request
        when(executor.schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("test")
        )).thenReturn("test");

        PathEdge src = new PathEdge("domain", "101001", "1");
        PathEdge dst = new PathEdge("domain", "102001", "1");

        TrafficClassifier classifier = new TrafficClassifier(
                "10.1.1.1",
                "10.2.1.1",
                "00:04:00:02:01:01",
                1
        );

        TrafficProfile profile = new TrafficProfile(1);

        InterDomainPath interDomainPath = new InterDomainPath(
                "inter",
                "inter",
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.SCHEDULED
        );

        IntraDomainPath intraPath = new IntraDomainPath(
                interDomainPath,
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.REQUESTED,
                "domain",
                null
        );

        plugin.teardownIntraDomainPath("inter", "domain_test", listener);
        verify(executor).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("DELETE_domain_test")
        );
        plugin.notifySuccess("DELETE_domain_test", makeService(ServiceStatus.ACTIVE).getBody(), Service.class);
        // TODO: not invoked, zero interactions.
        verify(listener).notifyIntraDomainPathModification(
                eq("inter"),
                eq("domain_test"),
                eq(PathLifecycleAction.TEARDOWN),
                eq(OperationResult.FAILED)
        );
    }

    @Test
    public void teardownIntraDomainPath() throws Exception {
        setupIntraDomainPath();
        // Setup response to the path request
        when(template.exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.DELETE),
                any(),
                eq(Object.class))
        ).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        //Setup response to status check request
        when(executor.schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("test")
        )).thenReturn("test");

        PathEdge src = new PathEdge("domain", "101001", "1");
        PathEdge dst = new PathEdge("domain", "102001", "1");

        TrafficClassifier classifier = new TrafficClassifier(
                "10.1.1.1",
                "10.2.1.1",
                "00:04:00:02:01:01",
                1
        );

        TrafficProfile profile = new TrafficProfile(1);

        InterDomainPath interDomainPath = new InterDomainPath(
                "inter",
                "inter",
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.SCHEDULED
        );

        IntraDomainPath intraPath = new IntraDomainPath(
                interDomainPath,
                src,
                dst,
                ConnectionType.POINT_TO_POINT,
                classifier,
                profile,
                PathStatus.REQUESTED,
                "domain",
                null
        );

        plugin.teardownIntraDomainPath("inter", "domain_test", listener);
        verify(executor).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("DELETE_domain_test")
        );
        plugin.notifySuccess("DELETE_domain_test", makeService(ServiceStatus.TERMINATING).getBody(), Service.class);
        verify(executor, times(2)).schedule_exchange(
                eq("http://127.0.0.1:8089/affinity/connection/test"),
                eq(HttpMethod.GET),
                any(),
                eq(Service.class),
                eq(plugin),
                any(),
                eq("DELETE_domain_test")
        );
        plugin.notifySuccess("DELETE_domain_test", makeService(ServiceStatus.DELETED).getBody(), Service.class);
        // TODO: not invoked, zero interactions.
        verify(listener).notifyIntraDomainPathModification(
                eq("inter"),
                eq("domain_test"),
                eq(PathLifecycleAction.TEARDOWN),
                eq(OperationResult.COMPLETED)
        );
    }

    private ResponseEntity<Response> makeResponse(ServiceStatus status, String id) {
        Response output = new Response();
        output.status = status;
        output.connectionId = id;
        ResponseEntity<Response> response = new ResponseEntity<>(output, HttpStatus.OK);
        assert response.getBody().equals(output);
        assert response.getStatusCode().equals(HttpStatus.OK);
        return response;
    }

    private ResponseEntity<Service> makeService(ServiceStatus status) {
        Service output = new Service();
        output.status = status;
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

}