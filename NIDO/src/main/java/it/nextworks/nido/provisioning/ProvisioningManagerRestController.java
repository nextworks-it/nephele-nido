
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.provisioning;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.nextworks.nido.common.exceptions.EntityAlreadyExistingException;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;
import it.nextworks.nido.path.PathDbWrapper;
import it.nextworks.nido.provisioning.topology.Domain;
import it.nextworks.nido.provisioning.topology.TopologyDbWrapper;



@RestController
@CrossOrigin
@RequestMapping("/nido/management")
public class ProvisioningManagerRestController {

	private static final Logger log = LoggerFactory.getLogger(ProvisioningManagerRestController.class);
	
	@Autowired
	private TopologyDbWrapper topologyDbWrapper;
	
	@Autowired
	private PathDbWrapper pathDbWrapper;
	
	@Autowired
	private ProvisioningManager provisioningManager;
	
	public ProvisioningManagerRestController() { }
	
	@RequestMapping(value = "/domain", method = RequestMethod.POST)
	@ApiOperation(value = "createDomain", nickname = "Add a new network domain that can be controlled by the system.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 201, message = "Success", response = String.class),
    		@ApiResponse(code = 400, message = "Bad request", response = String.class),
    		@ApiResponse(code = 409, message = "Conflict", response = String.class),
    		@ApiResponse(code = 500, message = "Server error", response = String.class)})
	public ResponseEntity<?> createDomain(@ApiParam(name = "domain", value = "Description of the domain to be created", required = true) @RequestBody Domain domain) {
		log.debug("Received request to create new domain: " + domain.toString());
		String domainId = domain.getDomainId();
		if (domainId == null) {
			log.debug("Received domain without ID - not acceptable");
			return new ResponseEntity<String>("Domain ID null", HttpStatus.BAD_REQUEST);
		}
		try {
			topologyDbWrapper.addDomain(domain);
		} catch (EntityAlreadyExistingException e) {
			return new ResponseEntity<String>("Domain ID already present", HttpStatus.CONFLICT);
		}
		try {
			provisioningManager.addDomainPlugin(domain, false);
			log.debug("Domain plugin correctly loaded");
			return new ResponseEntity<String>(domainId, HttpStatus.CREATED);
		} catch (Exception e) {
			log.error("Impossible to load plugin for the given domain. Removing domain from DB.");
			try {
				topologyDbWrapper.removeDomain(domainId);
				log.debug("Plugin removed from DB.");
			} catch (EntityNotFoundException ex) {
				log.error(ex.getMessage());
			}
			return new ResponseEntity<String>("Operation failed.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/domains", method = RequestMethod.GET)
	@ApiOperation(value = "getDomains", nickname = "Retrieve all the network domains controlled by the system.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 200, message = "Success", response = Domain.class, responseContainer = "List")})
	public ResponseEntity<?> getDomains() {
		log.debug("Received request to retrieve all the domains");
		List<Domain> domains = topologyDbWrapper.getAllDomains();
		return new ResponseEntity<List<Domain>>(domains, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/domain/{domainId}", method = RequestMethod.GET)
	@ApiOperation(value = "getDomain", nickname = "Retrieve a network domain with the given ID.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 200, message = "Success", response = Domain.class),
    		@ApiResponse(code = 400, message = "Bad request", response = String.class),
    		@ApiResponse(code = 404, message = "Not found", response = String.class)})
	public ResponseEntity<?> retrieveDomain(@ApiParam(name = "domainId", value = "ID of the domain to be retrieved", required = true) @PathVariable String domainId) {
		log.debug("Received query for domain " + domainId);
		if (domainId == null) {
			log.error("Received domain query without ID");
			return new ResponseEntity<String>("Query without domain ID", HttpStatus.BAD_REQUEST);
		}
		try {
			Domain domain = topologyDbWrapper.getDomain(domainId);
			log.info("Domain found");
			return new ResponseEntity<Domain>(domain, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<String>("Domain not available: " + e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/domain/{domainId}", method = RequestMethod.DELETE)
	@ApiOperation(value = "deleteDomain", nickname = "Delete a network domain with a given ID.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 204, message = "Success", response = Void.class),
    		@ApiResponse(code = 400, message = "Bad request", response = String.class),
    		@ApiResponse(code = 404, message = "Not found", response = String.class),
    		@ApiResponse(code = 409, message = "Conflict", response = String.class)})
	public ResponseEntity<?> deleteDomain(@ApiParam(name = "domainId", value = "ID of the domain to be removed", required = true) @PathVariable String domainId) {
		log.debug("Received delete request for domain " + domainId);
		if (domainId == null) {
			log.error("Received domain delete request without ID");
			return new ResponseEntity<String>("Delete request without domain ID", HttpStatus.BAD_REQUEST);
		}
		if (pathDbWrapper.isDomainInUse(domainId)) {
			log.debug("Domain " + domainId + " is still in use and it cannot be removed.");
			return new ResponseEntity<String>("Domain " + domainId + " is still in use and it cannot be removed.", HttpStatus.CONFLICT);
		}
		try {
			topologyDbWrapper.removeDomain(domainId);
			provisioningManager.removeDomainPlugin(domainId);
			log.debug("Removed domain plugin");
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (Exception e) {
			log.error("Domain not found in DB");
			return new ResponseEntity<String>("Domain not available", HttpStatus.NOT_FOUND);
		}
	}

}
