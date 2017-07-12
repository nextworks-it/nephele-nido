
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.engine;

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
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.PathDbWrapper;
import it.nextworks.nido.path.PathStatus;

@RestController
@CrossOrigin
@RequestMapping("/nido/operation")
public class EngineRestController {
	
	private static final Logger log = LoggerFactory.getLogger(EngineRestController.class);
	
	@Autowired
	MultiDomainConnectivityManager mdcManager;
	
	@Autowired
	PathDbWrapper pathDbWrapper; 

	public EngineRestController() {	}
	
	@RequestMapping(value = "/path", method = RequestMethod.POST)
	@ApiOperation(value = "createInterdomainPath", nickname = "Create a new inter-domain path")
    @ApiResponses(value = { 
    		@ApiResponse(code = 201, message = "Success", response = String.class),
    		@ApiResponse(code = 409, message = "Conflict", response = String.class),
    		@ApiResponse(code = 500, message = "Server error", response = String.class)})
	public ResponseEntity<?> createInterdomainPath(@ApiParam(name = "pathId", value = "Description of the path to be created", required = true) @RequestBody InterDomainPath path) {
		log.debug("Received request for interdomain path creation: " + path.toString());
		Long pathId = 0L;
		try {
			pathId = pathDbWrapper.createInterDomainPath(path);
		} catch (EntityAlreadyExistingException e) {
			log.error("Error while updating the DB.");
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
		}
		
		try {
			mdcManager.instantiateInterDomainPath(path);
			log.debug("Invoked MDC method to instantiate inter-domain path");
			return new ResponseEntity<String>(Long.toString(pathId), HttpStatus.CREATED);
		} catch (Exception e) {
			log.error("Failed inter domain path instantiation");
			try {
				pathDbWrapper.modifyInterDomainPathStatus(path.getInterDomainPathId(), PathStatus.FAILED);
				return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (Exception ex) {
				return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	@RequestMapping(value = "/paths", method = RequestMethod.GET)
	@ApiOperation(value = "retrievePaths", nickname = "Retrieve the list of all the inter-domain paths.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 200, message = "Success", response = InterDomainPath.class, responseContainer = "List")})
	public ResponseEntity<?> getInterdomainPaths() {
		log.debug("Received request to retrieve all the paths");
		List<InterDomainPath> paths = pathDbWrapper.retrieveAllInterDomainPaths();
		return new ResponseEntity<List<InterDomainPath>>(paths, HttpStatus.OK);
	}
	
	@ApiOperation(value = "retrievePath", nickname = "Retrieve an existing inter-domain path with a given ID.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 200, message = "Success", response = InterDomainPath.class),
    		@ApiResponse(code = 400, message = "Bad request", response = String.class)})
	@RequestMapping(value = "/path/{pathId}", method = RequestMethod.GET)
	public ResponseEntity<?> retrievePath(@ApiParam(name = "pathId", value = "ID of the path to be retrieved", required = true) @PathVariable String pathId) {
		log.debug("Received query for path " + pathId);
		if (pathId == null) {
			log.error("Received path query without ID");
			return new ResponseEntity<String>("Query without path ID", HttpStatus.BAD_REQUEST);
		}
		try {
			InterDomainPath path = pathDbWrapper.retrieveInterDomainPath(pathId);
			log.debug("Path found");
			return new ResponseEntity<InterDomainPath>(path, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			log.error("Path not found");
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/path/{pathId}", method = RequestMethod.DELETE)
	@ApiOperation(value = "deletePath", nickname = "Delete an existing inter-domain path with a given ID.")
    @ApiResponses(value = { 
    		@ApiResponse(code = 204, message = "Success", response = Void.class),
    		@ApiResponse(code = 400, message = "Bad request", response = String.class),
    		@ApiResponse(code = 404, message = "Not found", response = String.class),
    		@ApiResponse(code = 500, message = "Server error", response = String.class)})
	public ResponseEntity<?> teardownPath(@ApiParam(name = "pathId", value = "ID of the path to be torn down", required = true) @PathVariable String pathId) {
		log.debug("Received tear down request for path " + pathId);
		if (pathId == null) {
			log.error("Received path tear down request without ID");
			return new ResponseEntity<String>("Delete request without domain ID", HttpStatus.BAD_REQUEST);
		}
		try {
			mdcManager.teardownInterDomainPath(pathId);
			log.debug("Invoked MDC method to teardown path " + pathId);
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
