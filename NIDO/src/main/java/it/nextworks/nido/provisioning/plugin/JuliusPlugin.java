
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
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.provisioning.elements.JuliusConnectionRequest;
import it.nextworks.nido.provisioning.elements.JuliusConnectionResponse;
import it.nextworks.nido.provisioning.elements.JuliusRequest;
import it.nextworks.nido.provisioning.elements.JuliusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

public class JuliusPlugin extends ProvisioningPlugin {

	private static final Logger log = LoggerFactory.getLogger(JuliusPlugin.class);

	private static final PrimitiveIterator.OfInt iterator = IntStream.iterate(1, (i) -> i+1).iterator();

	private static int getCounter() {
		return iterator.next();
	}

	public JuliusPlugin(String domainId, String url) {
		super(ProvisioningPluginType.PLUGIN_JULIUS, domainId, url, ":9000/", ":8888/topology");
	}

	@Override
	public String setupIntraDomainPath(String interDomainPathId, IntraDomainPath path, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		log.info("Setting up sub path of '{}' in domain '{}'.", interDomainPathId, domainId);
		try {
			String srcId = path.getSourceEndPoint().getNodeId();
			String dstId = path.getDestinationEndPoint().getNodeId();
			if (!isNumeric(srcId)) {
				throw new GeneralFailureException(
						String.format("Received non numeric id '%s', in path %s.", srcId, interDomainPathId)
				);
			}
			if (!isNumeric(dstId)) {
				throw new GeneralFailureException(
						String.format("Received non numeric id '%s', in path %s.", dstId, interDomainPathId)
				);
			}
			int srcIntId = Integer.parseInt(srcId);
			int dstIntId = Integer.parseInt(dstId);
			JuliusConnectionRequest connection = new JuliusConnectionRequest(
					getCounter(),
					srcIntId,
					dstIntId,
					0,
					path.getTrafficProfile().getBandwidth()
			);

			JuliusRequest request = new JuliusRequest();
			request.demands.add(connection);
			String localId = postService(request, interDomainPathId);
			String globalId = globalize(localId);
			log.info("Sub path of '{}' on domain '{}' requested: id is '{}'.", interDomainPathId, domainId, globalId);
			return globalId;
		} catch (Exception e) {
			log.error("Error during intra domain path setup. {}: {}.", e.getClass().getSimpleName(), e.getMessage());
			log.debug("Exception details: ", e);
			throw new GeneralFailureException(e);
		}
	}

	private String postService(JuliusRequest req, String interDomainPathId)
			throws GeneralFailureException {
		try {
			String url = getControllerUrl() + "julius/lsp_request";
			HttpHeaders header = new HttpHeaders();
			header.add("Content-Type", "application/json");

			HttpEntity<JuliusRequest> entity = new HttpEntity<>(req, header);
			ResponseEntity<JuliusResponse> httpResponse = restTemplate.exchange(
					url,
					HttpMethod.POST,
					entity,
					JuliusResponse.class
			);
			switch (httpResponse.getStatusCode()) {
				case OK:
					List<JuliusConnectionResponse> responses = httpResponse.getBody().responses;
					if (null == responses) {
						throw new Exception(
								"Got null responses array from Julius."
						);
					} else if (responses.size() != 1) {
						throw new Exception(
								String.format(
										"%s connections found in response, one expected.",
										responses.size()
								)
						);
					}
					return responses.get(0).lspId.toString();

				default:
					throw new Exception(
							String.format("Julius responded on POST with code %s: %s.",
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

	@Override
	public void teardownIntraDomainPath(String interDomainPathId, String intraDomainPathId, PathNotificationListenerInterface listener)
			throws EntityNotFoundException, GeneralFailureException {
		log.info("Deleting path '{}' (sub path of '{}') in domain '{}'.",
				intraDomainPathId, interDomainPathId, domainId);
		try {
			String localId = unGlobalize(intraDomainPathId);

			try {
				String url = getControllerUrl() + "julius/lsp/" + localId;

				HttpEntity<Object> entity = new HttpEntity<>(null, null);
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
						break;
					default:
						throw new Exception(
								String.format("Julius responded on DELETE with code %s %s",
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
		} catch (Exception e) {
			log.error("Error during intra domain path teardown. {}: {}.", e.getClass().getSimpleName(), e.getMessage());
			log.debug("Exception details: ", e);
			listener.notifyIntraDomainPathModification(
					interDomainPathId,
					intraDomainPathId,
					PathLifecycleAction.TEARDOWN,
					OperationResult.FAILED
			);
			throw new GeneralFailureException(e);
		}
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

	private boolean isNumeric(String str) {
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(str, pos);
		return str.length() == pos.getIndex();
	}

}
