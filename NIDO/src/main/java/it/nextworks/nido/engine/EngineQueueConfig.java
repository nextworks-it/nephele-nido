
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.engine;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineQueueConfig {

	public static final String engineMessageQueue = "engine-queue";
	public static final String engineMessageExchange = "engine-exchange";
	
	@Bean(name=engineMessageExchange)
	TopicExchange exchange() {
		return new TopicExchange(engineMessageExchange, true, false);
	}

}
