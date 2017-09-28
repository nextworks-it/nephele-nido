
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
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.GeneralFailureException;
import it.nextworks.nido.engine.PathNotificationListenerInterface;
import it.nextworks.nido.path.ConnectionType;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.PathEdge;
import it.nextworks.nido.provisioning.elements.Connection;
import it.nextworks.nido.provisioning.elements.EndPoint;
import it.nextworks.nido.provisioning.elements.Recovery;
import it.nextworks.nido.provisioning.elements.Response;
import it.nextworks.nido.provisioning.elements.Service;
import it.nextworks.nido.provisioning.elements.ServiceStatus;
import it.nextworks.nido.provisioning.elements.TrafficProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OceaniaPlugin extends ProvisioningPlugin implements RestExecutorClientInterface {

    private static final Logger log = LoggerFactory.getLogger(OceaniaPlugin.class);

    private static final int POLLING_INTERVAL = 3;

    private RestExecutorInterface executor;

    private Map<String, PathData> pathId2Listener = new ConcurrentHashMap<>();

    public OceaniaPlugin(String domainId, String url) {
        this(domainId, url, new RestExecutor());
    }

    public OceaniaPlugin(String domainId, String url, RestExecutorInterface executor) {
        super(
                ProvisioningPluginType.PLUGIN_OCEANIA,
                domainId,
                url, ":8089/",
                ":8888/topology"
        );
        this.executor = executor;
    }

    public OceaniaPlugin(String domainId, String url, RestExecutorInterface executor, RestTemplate restTemplate) {
        super(
                ProvisioningPluginType.PLUGIN_OCEANIA,
                domainId,
                url, ":8089/",
                ":8888/topology",
                restTemplate
        );
        this.executor = executor;

    }

    @Override
    public String setupIntraDomainPath(String interDomainPathId, IntraDomainPath path,
                                       PathNotificationListenerInterface listener)
            throws EntityNotFoundException, GeneralFailureException {

        log.info("Setting up sub path of '{}' in domain '{}'.", interDomainPathId, domainId);

        String srcNodeId;
        Integer srcPort;
        String dstNodeId;
        Integer dstPort;

        try {
            checkInput(path);
            srcNodeId = path.getSourceEndPoint().getNodeId();
            srcPort = Integer.parseInt(path.getSourceEndPoint().getPortId());
            dstNodeId = path.getDestinationEndPoint().getNodeId();
            dstPort = Integer.parseInt(path.getDestinationEndPoint().getPortId());
        } catch (EntityNotFoundException | GeneralFailureException e) {
            log.error("Malformed request for path '{}' on domain '{}': {}.",
                    interDomainPathId, domainId, e.getMessage());
            throw e;
        }

        EndPoint reqSrc = new EndPoint(getPodId(srcNodeId), getTorId(srcNodeId), srcPort);
        EndPoint reqDst = new EndPoint(getPodId(dstNodeId), getTorId(dstNodeId), dstPort);

        TrafficProfile profile = new TrafficProfile(path.getTrafficProfile().getBandwidth());

        Connection connection = new Connection(
                reqSrc,
                reqDst,
                Recovery.UNPROTECTED,
                profile,
                path.getConnectionType(),
                path.getTrafficClassifier().getDstIpAddress()
        );

        Service request = new Service();
        request.addConnection(connection);
        String localId = postService(request, interDomainPathId);
        String globalId = globalize(localId);
        log.info("Sub path of '{}' on domain '{}' requested: id is '{}'.", interDomainPathId, domainId, globalId);

        scheduleStatusCheck(globalId, inSeconds(1));

        addData(globalId, listener, interDomainPathId);

        return globalId;
    }

    @Override
    public void teardownIntraDomainPath(String interDomainPathId, String intraDomainPathId,
                                        PathNotificationListenerInterface listener)
            throws EntityNotFoundException, GeneralFailureException {
        log.info("Deleting path '{}' (sub path of '{}') in domain '{}'.",
                intraDomainPathId, interDomainPathId, domainId);
        String localId = unGlobalize(intraDomainPathId);

        try {
            String url = getControllerUrl() + "affinity/connection/" + localId;

            HttpEntity<Service> entity = new HttpEntity<>(null, null);
            ResponseEntity<?> httpResponse = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Object.class
            );
            switch (httpResponse.getStatusCode()) {
                case OK:
                    log.info("Delete request for path '{}' (sub path of '{}') successfully sent.",
                            intraDomainPathId, interDomainPathId);
                    scheduleStatusCheck("DELETE_" + intraDomainPathId, inSeconds(1));
                    break;
                default:
                    throw new Exception(
                            String.format("Application affinity service responded on DELETE with code %s %s",
                                    httpResponse.getStatusCode().value(),
                                    httpResponse.getStatusCode().getReasonPhrase())
                    );
            }
        } catch (Exception e) {
            log.debug("Exception details: ", e);
            throw new GeneralFailureException(
                    String.format("Error while deleting path '%s' on domain '%s'. %s: %s",
                            intraDomainPathId, domainId, e.getClass().getSimpleName(), e.getMessage())
            );
        }
    }

    private Date inSeconds(int seconds) {
        Instant now = Instant.now();
        return Date.from(now.plusSeconds(seconds));
    }

    private String globalize(String localId) {
        return domainId + "_" + localId;
    }

    private String unGlobalize(String globalId) {
        String temp;
        if (globalId.startsWith("DELETE_")) {
            temp = globalId.substring("DELETE_".length());
        } else {
            temp = globalId;
        }
        return temp.substring((domainId + "_").length());
    }

    private String postService(Service service, String interDomainPathId)
            throws GeneralFailureException {
        try {
            String url = getControllerUrl() + "affinity/connection";
            HttpHeaders header = new HttpHeaders();
            header.add("Content-Type", "application/json");

            HttpEntity<Service> entity = new HttpEntity<>(service, header);
            ResponseEntity<Response> httpResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Response.class
            );
            switch (httpResponse.getStatusCode()) {
                case OK:
                    return httpResponse.getBody().connectionId;

                default:
                    throw new Exception(
                            String.format("Application affinity service responded on POST with code %s: %s.",
                                    httpResponse.getStatusCode().value(),
                                    httpResponse.getStatusCode().getReasonPhrase())
                    );
            }
        } catch (Exception e) {
            log.debug("Exception details: ", e);
            throw new GeneralFailureException(
                    String.format("Error while posting path %s on domain %s. %s: %s.",
                            interDomainPathId, domainId, e.getClass().getSimpleName(), e.getMessage())
            );
        }
    }

    private void scheduleStatusCheck(String intraDomainPathId, Date date) {
        String localId = unGlobalize(intraDomainPathId);
        String url = getControllerUrl() + "affinity/connection/" + localId;
        HttpHeaders header = new HttpHeaders();
        header.add("Accept", "application/json");

        log.debug("Scheduling status check for path '{}'.", intraDomainPathId);

        HttpEntity<Service> entity = new HttpEntity<>(header);
        executor.schedule_exchange(
                url,
                HttpMethod.GET,
                entity,
                Service.class,
                this,
                date,
                intraDomainPathId
        );
    }

    @Override
    public <T> void notifySuccess(String intraDomainPathId, T body, Class<T> responseType) {
        if (!responseType.equals(Service.class)) {
            log.error("Status check returned unexpected payload of type {}", responseType);
            scheduleStatusCheck(intraDomainPathId, inSeconds(POLLING_INTERVAL));
            return;
        }

        ServiceStatus status = ((Service) body).status;
        PathData pathData;
        boolean deleting = false;

        String actualIntraDomainId;
        if (intraDomainPathId.startsWith("DELETE_")) {
             actualIntraDomainId = intraDomainPathId.substring("DELETE_".length());
             deleting = true;
        } else {
            actualIntraDomainId = intraDomainPathId;
        }

        try {
            pathData = getPathData(actualIntraDomainId);
        } catch (EntityNotFoundException exc) {
            // It's enough to just quit, we already logged the error.
            return;
        }
        PathNotificationListenerInterface listener = pathData.listener;
        String interDomainPathId = pathData.interDomainPathId;

        if (deleting) {
            parseTearDownStatus(status, actualIntraDomainId, interDomainPathId, listener);
        } else {
            parseSetupStatus(status, actualIntraDomainId, interDomainPathId, listener);
        }

    }

    private void parseTearDownStatus(ServiceStatus status,
                                  String intraDomainPathId,
                                  String interDomainPathId,
                                  PathNotificationListenerInterface listener) {
        switch (status) {
            case DELETED:
                log.info("Deleted path '{}' (sub path of '{}').", intraDomainPathId, interDomainPathId);
                try {
                    listener.notifyIntraDomainPathModification(
                            interDomainPathId,
                            intraDomainPathId,
                            PathLifecycleAction.TEARDOWN,
                            OperationResult.COMPLETED
                    );
                } catch (EntityNotFoundException exc) {
                    log.error("Could not notify completion of the setup of path '{}' (sub path of '{}'): {}",
                            intraDomainPathId, interDomainPathId, exc.getMessage());
                }
                break;
            case TERMINATING:
                log.debug("Path '{}' not yet deleted. Scheduling status poll.", intraDomainPathId);
                scheduleStatusCheck("DELETE_" + intraDomainPathId, inSeconds(POLLING_INTERVAL));
                break;
            default:
                log.error("Unexpected status {} in tear-down phase of path '{}'.", status, intraDomainPathId);
                log.error("Tear-down of path '{}' (sub path of '{}') failed.", intraDomainPathId, interDomainPathId);
                try {
                    listener.notifyIntraDomainPathModification(
                            interDomainPathId,
                            intraDomainPathId,
                            PathLifecycleAction.TEARDOWN,
                            OperationResult.FAILED
                    );
                } catch (EntityNotFoundException exc) {
                    log.error("Could not notify failure of the tear-down of path '{}' (sub path of '{}'): {}",
                            intraDomainPathId, interDomainPathId, exc.getMessage());
                }
        }
    }

    private void parseSetupStatus(ServiceStatus status,
                                  String intraDomainPathId,
                                  String interDomainPathId,
                                  PathNotificationListenerInterface listener) {
        switch (status) {
            case ACTIVE:
                log.info("Established path '{}' (sub path of '{}').", intraDomainPathId, interDomainPathId);
                try {
                    listener.notifyIntraDomainPathModification(
                            interDomainPathId,
                            intraDomainPathId,
                            PathLifecycleAction.SETUP,
                            OperationResult.COMPLETED
                    );
                } catch (EntityNotFoundException exc) {
                    log.error("Could not notify completion of the setup of path '{}' (sub path of '{}'): {}",
                            intraDomainPathId, interDomainPathId, exc.getMessage());
                }
                break;
            case SCHEDULED:
            case ESTABLISHING:
                log.debug("Path '{}' not yet established. Scheduling status poll.", intraDomainPathId);
                scheduleStatusCheck(intraDomainPathId, inSeconds(POLLING_INTERVAL));
                break;
            default:
                log.error("Unexpected status {} in setup phase of path '{}'.", status, intraDomainPathId);
                log.error("Setup of path '{}' (sub path of '{}') failed.", intraDomainPathId, interDomainPathId);
                try {
                    listener.notifyIntraDomainPathModification(
                            interDomainPathId,
                            intraDomainPathId,
                            PathLifecycleAction.SETUP,
                            OperationResult.FAILED
                    );
                } catch (EntityNotFoundException exc) {
                    log.error("Could not notify failure of the setup of path '{}' (sub path of '{}'): {}",
                            intraDomainPathId, interDomainPathId, exc.getMessage());
                }
        }
    }

    @Override
    public void notifyFailure(String intraDomainPathId, String message) {
        try {
            getPathData(intraDomainPathId);
        } catch (EntityNotFoundException e) {
            // It's enough to just quit, we already logged the error.
            return;
        }
        log.error("Status check for path '{}' failed: {}. Retrying.",
                intraDomainPathId, message);
        scheduleStatusCheck(intraDomainPathId, inSeconds(POLLING_INTERVAL));
    }

    @Override
    public void notifyFailure(String intraDomainPathId, Throwable exc) {
        try {
            getPathData(intraDomainPathId);
        } catch (EntityNotFoundException e) {
            // It's enough to just quit, we already logged the error.
            return;
        }
        log.error("Status check for path '{}' failed: {}. Retrying.",
                intraDomainPathId, exc.getMessage());
        scheduleStatusCheck(intraDomainPathId, inSeconds(POLLING_INTERVAL));
    }

    @Override
    public void notifyFailure(String intraDomainPathId, String message, Throwable exc) {
        try {
            getPathData(intraDomainPathId);
        } catch (EntityNotFoundException e) {
            // It's enough to just quit, we already logged the error.
            return;
        }
        log.error("Status check for path '{}' failed with exception {} and error {}. Retrying.",
                intraDomainPathId, exc.getClass().getSimpleName(), message);
        scheduleStatusCheck(intraDomainPathId, inSeconds(POLLING_INTERVAL));
    }

    private Integer getPodId(String nodeId) throws GeneralFailureException {
        try {
            return Integer.parseInt(nodeId.substring(1, 3));
        } catch (StringIndexOutOfBoundsException exc) {
            throw new GeneralFailureException(String.format("Malformed node id '%s'.", nodeId));
        }
    }

    private Integer getTorId(String nodeId) throws GeneralFailureException {
        try {
            return Integer.parseInt(nodeId.substring(3, 5));
        } catch (StringIndexOutOfBoundsException exc) {
            throw new GeneralFailureException(String.format("Malformed node id '%s'.", nodeId));
        }
    }

    private void checkInput(IntraDomainPath path)
            throws EntityNotFoundException, GeneralFailureException {
        if (!path.getDomainId().equals(domainId)) {
            throw new GeneralFailureException(
                    String.format("Path for domain %s requested to domain %s.", path.getDomainId(), domainId)
            );
        }
        if (!path.getConnectionType().equals(ConnectionType.POINT_TO_POINT)) {
            throw new GeneralFailureException(
                    String.format("Connection type %s not (yet) supported.", path.getConnectionType())
            );
        }
        checkEndPoint(path.getSourceEndPoint());
        checkEndPoint(path.getDestinationEndPoint());
    }

    private void checkEndPoint(PathEdge endPoint)
            throws EntityNotFoundException, GeneralFailureException {
        if (!isNumeric(endPoint.getNodeId())) {
            throw new EntityNotFoundException(
                    String.format("Non numeric node ID received: %s.", endPoint.getNodeId())
            );
        }
        if (!endPoint.getNodeId().startsWith("1")) {
            throw new EntityNotFoundException(
                    String.format("Non-ToR ID passed as endpoint: %s.", endPoint.getNodeId())
            );
        }
        if (!isNumeric(endPoint.getPortId())) {
            throw new EntityNotFoundException(
                    String.format("Non numeric port ID received: %s.", endPoint.getPortId())
            );
        }
        if (!endPoint.getDomainId().equals(domainId)) {
            throw new GeneralFailureException(
                    String.format("Node in domain %s found in request for domain %s.",
                            endPoint.getDomainId(), domainId)
            );
        }
    }

    private boolean isNumeric(String str) {
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

    private void addData(String intraDomainPathId, PathNotificationListenerInterface listener, String interDomainPathId) {
        PathData pathData = new PathData(listener, interDomainPathId);
        pathId2Listener.put(intraDomainPathId, pathData);
    }

    private static class PathData {
        private PathNotificationListenerInterface listener;
        private String interDomainPathId;

        private PathData(PathNotificationListenerInterface listener, String interDomainPathId) {
            this.listener = listener;
            this.interDomainPathId = interDomainPathId;
        }

        @Override
        public String toString() {
            return listener.toString() + "; " + interDomainPathId;
        }
    }

    private PathData getPathData(String intraDomainPathId) throws EntityNotFoundException {
        PathData pathData = pathId2Listener.get(intraDomainPathId);
        if (null == pathData) {
            log.warn("Received spurious notification for unknown path '{}'.", intraDomainPathId);
            throw new EntityNotFoundException();
        }
        return pathData;
    }
}
