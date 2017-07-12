
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.Date;

/**
 * Created by Marco Capitani on 06/07/17.
 *
 * @author Marco Capitani (m.capitani AT nextworks.it)
 */
public class RestExecutor implements RestExecutorInterface {

    private static final Logger log = LoggerFactory.getLogger(RestExecutor.class);

    private AsyncRestTemplate template = new AsyncRestTemplate();
    private TaskScheduler scheduler = new ConcurrentTaskScheduler();

    public RestExecutor() {
    }

    private <T> SuccessCallback<ResponseEntity<T>> getSuccessCallback(String opId,
                                                                      Class<T> responseType,
                                                                      RestExecutorClientInterface client,
                                                                      HttpMethod method,
                                                                      String url) {
        return response -> {
            if (response.getStatusCode().is2xxSuccessful()) {
                client.notifySuccess(opId, response.getBody(), responseType);
            } else {
                client.notifyFailure(
                        opId,
                        String.format("%s request to url %s got error code %s: %s",
                                method,
                                url,
                                response.getStatusCode().value(),
                                response.getStatusCode().getReasonPhrase())
                );
            }
        };
    }

    private FailureCallback getFailureCallback(String opId,
                                               RestExecutorClientInterface client) {
        return throwable -> client.notifyFailure(opId, throwable);
    }

    @Override
    public <T> String submit_exchange(String url,
                                      HttpMethod method,
                                      HttpEntity<?> requestEntity,
                                      Class<T> responseType,
                                      RestExecutorClientInterface client,
                                      String opId) {
        log.debug("Enqueuing request {} to url {}. Expected response type: {}.", opId, url, responseType);
        ListenableFuture<ResponseEntity<T>> future = template.exchange(url, method, requestEntity, responseType);
        future.addCallback(
                getSuccessCallback(opId, responseType, client, method, url),
                getFailureCallback(opId, client)
        );
        return opId;
    }

    @Override
    public <T> String schedule_exchange(String url,
                                        HttpMethod method,
                                        HttpEntity<?> requestEntity,
                                        Class<T> responseType,
                                        RestExecutorClientInterface client,
                                        Date date,
                                        String opId) {
        scheduler.schedule(
                () ->
                    this.submit_exchange(
                            url,
                            method,
                            requestEntity,
                            responseType,
                            client,
                            opId
                    ),
                date
        );
        return opId;
    }
}
