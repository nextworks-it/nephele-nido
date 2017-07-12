
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido.path;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterDomainPathRepository extends JpaRepository<InterDomainPath, Long> {

	Optional<InterDomainPath> findById(Long id);
	Optional<InterDomainPath> findByInterDomainPathId(String id);
	
}
