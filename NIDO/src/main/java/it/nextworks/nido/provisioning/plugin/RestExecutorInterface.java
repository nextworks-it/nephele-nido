
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning.plugin;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Marco Capitani on 06/07/17.
 *
 * @author Marco Capitani (m.capitani AT nextworks.it)
 */
public interface RestExecutorInterface {

    default <T> String submit_exchange(String url,
                                       HttpMethod method,
                                       HttpEntity<?> requestEntity,
                                       Class<T> responseType,
                                       RestExecutorClientInterface client) {
        String uuid = UUID.randomUUID().toString();
        return submit_exchange(
                url,
                method,
                requestEntity,
                responseType,
                client,
                uuid
        );
    }

    <T> String submit_exchange(String url,
                               HttpMethod method,
                               HttpEntity<?> requestEntity,
                               Class<T> responseType,
                               RestExecutorClientInterface client,
                               String opId);

    default <T> String schedule_exchange(String url,
                                         HttpMethod method,
                                         HttpEntity<?> requestEntity,
                                         Class<T> responseType,
                                         RestExecutorClientInterface client,
                                         Date date) {
        String opId = UUID.randomUUID().toString();
        return schedule_exchange(
                url,
                method,
                requestEntity,
                responseType,
                client,
                date,
                opId
        );
    }

    <T> String schedule_exchange(String url,
                                 HttpMethod method,
                                 HttpEntity<?> requestEntity,
                                 Class<T> responseType,
                                 RestExecutorClientInterface client,
                                 Date date,
                                 String opId);

}
