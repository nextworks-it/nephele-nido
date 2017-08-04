
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nextworks.nido.common.exceptions.EntityAlreadyExistingException;
import it.nextworks.nido.common.exceptions.EntityNotFoundException;

@Service
public class PathDbWrapper {

	private static final Logger log = LoggerFactory.getLogger(PathDbWrapper.class);
	
	@Autowired
	InterDomainPathRepository interDomainPathRepository;
	
	@Autowired
	IntraDomainPathRepository intraDomainPathRepository;
	
	public PathDbWrapper() { }
	
	public synchronized Long createInterDomainPath(InterDomainPath path)
	throws EntityAlreadyExistingException {
		log.debug("Received request to add a new inter domain path in DB.");
		if (interDomainPathRepository.findByInterDomainPathId(path.getInterDomainPathId()).isPresent()) {
			log.error("Inter domain path already existing. Impossible to create a new one.");
			throw new EntityAlreadyExistingException("Inter domain path already existing. Impossible to create a new one.");
		}
		InterDomainPath idp = new InterDomainPath(path.getInterDomainPathId(), path.getName(), 
				path.getSourceEndPoint(), path.getDestinationEndPoint(), path.getConnectionType(), path.getTrafficClassifier(), 
				path.getTrafficProfile(), PathStatus.REQUESTED);
		interDomainPathRepository.saveAndFlush(idp);
		log.debug("Inter domain path added in repo.");
		return idp.getId();
	}
	
	public synchronized void modifyInterDomainPathStatus(String interDomainPathId, PathStatus status) throws EntityNotFoundException {
		InterDomainPath idp = retrieveInterDomainPath(interDomainPathId);
		idp.setPathStatus(status);
		interDomainPathRepository.saveAndFlush(idp);
	}
	
	public synchronized void removeInterDomainPath(String interDomainPathId) throws EntityNotFoundException {
		InterDomainPath idp = retrieveInterDomainPath(interDomainPathId);
		interDomainPathRepository.delete(idp);
		log.debug("Inter domain path " + interDomainPathId + " removed from DB.");
	}
	
	public InterDomainPath retrieveInterDomainPath(String interDomainPathId) throws EntityNotFoundException {
		Optional<InterDomainPath> idpOpt = interDomainPathRepository.findByInterDomainPathId(interDomainPathId);
		if (idpOpt.isPresent()) {
			return idpOpt.get();
		} else throw new EntityNotFoundException("Inter domain path with path ID " + interDomainPathId + " not found");
	}
	
	public List<InterDomainPath> retrieveAllInterDomainPaths() {
		return interDomainPathRepository.findAll();
	}
	
	public boolean isDomainInUse(String domainId) {
		log.debug("Checking if domain " + domainId + " is in use");
		List<PathStatus> acceptableStatus = new ArrayList<>();
		acceptableStatus.add(PathStatus.DELETED);
		acceptableStatus.add(PathStatus.FAILED);
		List<IntraDomainPath> paths = intraDomainPathRepository.findByDomainIdAndPathStatusNotIn(domainId, acceptableStatus);
		if (paths.isEmpty()) {
			log.debug("No active paths found in domain " + domainId + ". The domain is not in use.");
			return false;
		}
		return true;
	}
	
	public synchronized void createIntraDomainPath(String interDomainPathId, IntraDomainPath intraDomainPath) 
			throws EntityNotFoundException, EntityAlreadyExistingException {
		Optional<IntraDomainPath> intraPathOpt = intraDomainPathRepository.findByInternalId(intraDomainPath.getInternalId());
		if (intraPathOpt.isPresent()) {
			log.error("Intra domain path with given ID already existing. Impossible to create a new one.");
			throw new EntityAlreadyExistingException("Intra domain path with given ID already existing. Impossible to create a new one.");
		}
		InterDomainPath idp = retrieveInterDomainPath(interDomainPathId);
		IntraDomainPath intraPath = new IntraDomainPath(idp, 
				intraDomainPath.getSourceEndPoint(),
				intraDomainPath.getDestinationEndPoint(), 
				intraDomainPath.getConnectionType(), 
				intraDomainPath.getTrafficClassifier(), 
				intraDomainPath.getTrafficProfile(), 
				PathStatus.SCHEDULED, 
				intraDomainPath.getDomainId(), 
				intraDomainPath.getInternalId());
		intraDomainPathRepository.saveAndFlush(intraPath);
		log.debug("IntraDomain path added in DB");
	}
	
	public synchronized void modifyIntraDomainPathStatus(String intraDomainPathId, PathStatus status) throws EntityNotFoundException {
		IntraDomainPath idp = retrieveIntraDomainPath(intraDomainPathId);
		idp.setPathStatus(status);
		intraDomainPathRepository.saveAndFlush(idp);
	}
	
	public IntraDomainPath retrieveIntraDomainPath(String intraDomainPathId) throws EntityNotFoundException {
		Optional<IntraDomainPath> intraPathOpt = intraDomainPathRepository.findByInternalId(intraDomainPathId);
		if (intraPathOpt.isPresent()) {
			return intraPathOpt.get();
		} else throw new EntityNotFoundException("Intra domain path with internal path ID " + intraDomainPathId + " not found.");
	}
	
	public boolean isAllIntraDomainPathsInStatus(String interDomainPathId, PathStatus status) throws EntityNotFoundException {
		List<IntraDomainPath> intraPaths = intraDomainPathRepository.findByIdpInterDomainPathId(interDomainPathId);
		if (intraPaths.size() == 0) throw new EntityNotFoundException("Intra domain paths not found for inter domain path " + interDomainPathId);
		for (IntraDomainPath p : intraPaths) {
			if (p.getPathStatus() != status) return false;
		}
		return true;
	}

}
