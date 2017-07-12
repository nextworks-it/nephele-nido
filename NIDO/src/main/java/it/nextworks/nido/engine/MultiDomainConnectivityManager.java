
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.engine;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.nextworks.nido.common.enums.OperationResult;
import it.nextworks.nido.common.enums.PathLifecycleAction;
import it.nextworks.nido.common.exceptions.EntityAlreadyExistingException;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.common.exceptions.WrongInternalStatusException;
import it.nextworks.nido.engine.message.EngineMessage;
import it.nextworks.nido.engine.message.InterDomainPathSetupMessage;
import it.nextworks.nido.engine.message.InterDomainPathTearDownMessage;
import it.nextworks.nido.engine.message.IntraDomainProvisioningResultMessage;
import it.nextworks.nido.engine.message.PathComputationResultMessage;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.PathDbWrapper;
import it.nextworks.nido.path.PathStatus;
import it.nextworks.nido.pathcomputation.MdPathComputationManager;
import it.nextworks.nido.provisioning.ProvisioningManager;
import it.nextworks.nido.provisioning.topology.TopologyManager;

@Service
public class MultiDomainConnectivityManager implements PathNotificationListenerInterface {
	
	private static final Logger log = LoggerFactory.getLogger(MultiDomainConnectivityManager.class);
	
	@Value("${spring.rabbitmq.host}")
	private String rabbitHost;
	
	@Autowired
	@Qualifier(EngineQueueConfig.engineMessageExchange)
	TopicExchange messageExchange;
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	//Key: interdomain path ID
	private Map<String, MdConnectionManager> connectionManagers = new HashMap<>();
	
	@Autowired
	PathDbWrapper pathDbWrapper;
	
	@Autowired
	MdPathComputationManager computationManager;
	
	@Autowired
	private ProvisioningManager provisioningManager;
	
	@Autowired
	private TopologyManager topologyManager;
	
	private Queue queue;
	
	public MultiDomainConnectivityManager() { }
	
	public void instantiateInterDomainPath(InterDomainPath path) throws EntityAlreadyExistingException, WrongInternalStatusException, Exception {
		log.debug("Received request to instantiate a new inter domain path");
		String interDomainPathId = path.getInterDomainPathId();
		if (connectionManagers.containsKey(interDomainPathId)) {
			log.error("Received request to instantiate an already existing connection.");
			throw new EntityAlreadyExistingException("Interdomain path " + interDomainPathId + " already existing.");
		}
		MdConnectionManager connectionManager = new MdConnectionManager(interDomainPathId, this, pathDbWrapper, computationManager, 
				provisioningManager, topologyManager, path);
		this.connectionManagers.put(interDomainPathId, connectionManager);
		String topic = "path." + interDomainPathId + ".setup";
		InterDomainPathSetupMessage message = new InterDomainPathSetupMessage(interDomainPathId, path);
		ObjectMapper mapper = buildObjectMapper();
		try {
			String json = mapper.writeValueAsString(message);
			rabbitTemplate.convertAndSend(messageExchange.getName(), topic, json);
			log.debug("Sent internal message with request to instantiate inter domain path " + interDomainPathId);
		} catch (JsonProcessingException e) {
			log.error("Error while translating internal path setup message in Json format.");
			pathDbWrapper.modifyInterDomainPathStatus(interDomainPathId, PathStatus.FAILED);
		}
	}
	
	public void teardownInterDomainPath(String pathId) throws EntityNotFoundException, WrongInternalStatusException, Exception {
		log.debug("Received request to teardown inter domain path " + pathId);
		if (connectionManagers.containsKey(pathId)) {
			String topic = "path." + pathId + ".teardown";
			InterDomainPathTearDownMessage message = new InterDomainPathTearDownMessage(pathId);
			ObjectMapper mapper = buildObjectMapper();
			try {
				String json = mapper.writeValueAsString(message);
				rabbitTemplate.convertAndSend(messageExchange.getName(), topic, json);
				log.debug("Sent internal message with request to tear down inter domain path " + pathId);
			} catch (JsonProcessingException e) {
				log.error("Error while translating internal path teardown message in Json format.");
				pathDbWrapper.modifyInterDomainPathStatus(pathId, PathStatus.FAILED);
			}	
		} else {
			log.error("Inter domain path ID " + pathId + " not found");
			throw new EntityNotFoundException("Inter domain path ID " + pathId + " not found");
		}
	}
	
	@Override
	public void notifyPathComputationResult(String interDomainPathId,
			OperationResult result,
			InterDomainPath path) throws EntityNotFoundException {
		log.debug("Received notification about inter domain path computation result for path " + interDomainPathId);
		if (connectionManagers.containsKey(interDomainPathId)) {
			String topic = "path." + interDomainPathId + ".notifyPathComputation";
			PathComputationResultMessage message = new PathComputationResultMessage(interDomainPathId, path, result);
			ObjectMapper mapper = buildObjectMapper();
			try {
				String json = mapper.writeValueAsString(message);
				rabbitTemplate.convertAndSend(messageExchange.getName(), topic, json);
				log.debug("Sent internal message with notification about path computation result for path " + interDomainPathId);
			} catch (JsonProcessingException e) {
				log.error("Error while translating internal path computation notification in Json format.");
				pathDbWrapper.modifyInterDomainPathStatus(interDomainPathId, PathStatus.FAILED);
			}	
		} else {
			log.error("Inter domain path ID " + interDomainPathId + " not found. Skipping message.");
			throw new EntityNotFoundException("Inter domain path ID " + interDomainPathId + " not found");
		}
	}
	
	@Override
	public void notifyIntraDomainPathModification(String interDomainPathId,
			String intraDomainPathId, 
			PathLifecycleAction pathLifecycleAction, 
			OperationResult result) throws EntityNotFoundException {
		log.debug("Received notification about intra domain path modification for inter-domain path " + interDomainPathId);
		if (connectionManagers.containsKey(interDomainPathId)) {
			String topic = "path." + interDomainPathId + ".notifyIntraPathModification";
			IntraDomainProvisioningResultMessage message = new IntraDomainProvisioningResultMessage(interDomainPathId, intraDomainPathId, result, pathLifecycleAction);
			ObjectMapper mapper = buildObjectMapper();
			try {
				String json = mapper.writeValueAsString(message);
				rabbitTemplate.convertAndSend(messageExchange.getName(), topic, json);
				log.debug("Sent internal message with notification about intra-domain path provisioning result for inter-domain path " + interDomainPathId);
			} catch (JsonProcessingException e) {
				log.error("Error while translating internal path provisioning notification in Json format.");
				pathDbWrapper.modifyInterDomainPathStatus(interDomainPathId, PathStatus.FAILED);
			}	
		} else {
			log.error("Inter domain path ID " + interDomainPathId + " not found. Skipping message.");
			throw new EntityNotFoundException("Inter domain path ID " + interDomainPathId + " not found");
		}
	}
	
	
	public void receiveMessage(String message) {
		try {
			log.debug("Received message on connectivity manager queue: \n" + message);
			ObjectMapper objectMapper = new ObjectMapper();
			EngineMessage msg = (EngineMessage) objectMapper.readValue(message, EngineMessage.class);
			String interDomainPathId = msg.getInterDomainPathId();
			if (!(connectionManagers.containsKey(interDomainPathId))) {
				log.error("Connection manager for inter domain path " + interDomainPathId + " not found. Skipping message");
				return;
			}
			MdConnectionManager mdConnectionManager = connectionManagers.get(interDomainPathId);
			switch (msg.getType()) {
			case SETUP_PATH_REQUEST: {
				log.debug("Received setup path request on connectivity manager queue.");
				InterDomainPathSetupMessage specMsg = (InterDomainPathSetupMessage) msg;
				mdConnectionManager.setupInterDomainPath(specMsg);
				break;
			}

			case PATH_COMPUTATION_RESULT: {
				log.debug("Received path computation result on connectivity manager queue.");
				PathComputationResultMessage specMsg = (PathComputationResultMessage) msg;
				mdConnectionManager.notifyPathComputationResult(specMsg);
				break;
			}

			case INTRA_DOMAIN_PROVISIONING_RESULT: {
				log.debug("Received intra domain provisioning result on connectivity manager queue.");
				IntraDomainProvisioningResultMessage specMsg = (IntraDomainProvisioningResultMessage) msg;
				mdConnectionManager.notifyIntraDomainPathProvisioning(specMsg);
				break;
			}

			case TEARDOWN_PATH_REQUEST: {
				log.debug("Received tear down path request on connectivity manager queue.");
				InterDomainPathTearDownMessage specMsg = (InterDomainPathTearDownMessage) msg;
				mdConnectionManager.tearDownInterDomainPath(specMsg);
				break;
			}

			default: {
				log.error("Received unknown message type on connectivity manager queue. Skipping it.");
				break;
			}
			} 
		} catch (Exception e) {
			log.error("Error while receiving message: " + e.getMessage());
		}
	
	}
	
	@PostConstruct
	private void createQueue() {
		String queueName = EngineQueueConfig.engineMessageQueue;
		log.debug("Creating new Queue " + queueName + " in rabbit host " + rabbitHost);
		CachingConnectionFactory cf = new CachingConnectionFactory();
		cf.setAddresses(rabbitHost);
		cf.setConnectionTimeout(5);
		RabbitAdmin rabbitAdmin = new RabbitAdmin(cf);
		this.queue = new Queue(queueName, false, false, true);
		rabbitAdmin.declareQueue(queue);
		rabbitAdmin.declareExchange(messageExchange);
		//binding: path.<pathId>.<pathAction> with pathAction: setup/teardown
		rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(messageExchange).with("path.*.*"));
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
		MessageListenerAdapter adapter = new MessageListenerAdapter(this, "receiveMessage");
		container.setMessageListener(adapter);
	    container.setQueueNames(queueName);
	    container.start();
	    log.debug("Queue created");
	}
	
	private ObjectMapper buildObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		return mapper;
	}

}
