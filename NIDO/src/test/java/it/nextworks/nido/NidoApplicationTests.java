
/*
 * Nextworks S.r.l.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package it.nextworks.nido;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import it.nextworks.nido.path.ConnectionType;
import it.nextworks.nido.path.InterDomainPath;
import it.nextworks.nido.path.InterDomainPathRepository;
import it.nextworks.nido.path.IntraDomainPath;
import it.nextworks.nido.path.IntraDomainPathRepository;
import it.nextworks.nido.path.PathEdge;
import it.nextworks.nido.path.PathStatus;
import it.nextworks.nido.path.TrafficClassifier;
import it.nextworks.nido.path.TrafficProfile;
import it.nextworks.nido.provisioning.topology.DomainsRepository;
import it.nextworks.nido.provisioning.topology.NodeRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@WebAppConfiguration
public class NidoApplicationTests {

	@Autowired
	InterDomainPathRepository interDomainPathRepository;
	
	@Autowired
	IntraDomainPathRepository intraDomainPathRepository;
	
	@Autowired
	private DomainsRepository domainsRepository;
	
	@Autowired
	private NodeRepository nodeRepository;
	
	@Autowired
    private WebApplicationContext webApplicationContext;
	
	@Test
	public void contextLoads() { }
	
	@Before
	public void setupTest() {
		cleanDb();
		
		InterDomainPath interDomainPath = new InterDomainPath("inter-01", "path-01", new PathEdge("domain-1", "node-1-1", "port-1-1-1"), 
				new PathEdge("domain-3", "node-3-2", "port-3-2-2"), 
				ConnectionType.POINT_TO_POINT, 
				new TrafficClassifier("10.0.0.1", "10.0.1.1", null, 0),
				new TrafficProfile(20),
				PathStatus.REQUESTED);
		
		interDomainPathRepository.saveAndFlush(interDomainPath);
		
		IntraDomainPath intraDomainPath1 = new IntraDomainPath(interDomainPath, 
				new PathEdge("domain-1", "node-1-1", "port-1-1-1"), 
				new PathEdge("domain-1", "node-1-2", "port-1-2-2"), 
				ConnectionType.POINT_TO_POINT, 
				new TrafficClassifier("10.0.0.1", "10.0.1.1", null, 0),
				new TrafficProfile(20),
				PathStatus.REQUESTED, 
				"domain-1", 
				"path-1-1");
		
		intraDomainPathRepository.saveAndFlush(intraDomainPath1);
		
		IntraDomainPath intraDomainPath2 = new IntraDomainPath(interDomainPath, 
				new PathEdge("domain-2", "node-2-1", "port-2-1-1"), 
				new PathEdge("domain-2", "node-2-2", "port-2-2-2"), 
				ConnectionType.POINT_TO_POINT, 
				new TrafficClassifier("10.0.0.1", "10.0.1.1", null, 0),
				new TrafficProfile(20),
				PathStatus.REQUESTED, 
				"domain-2", 
				"path-2-1");
		
		intraDomainPathRepository.saveAndFlush(intraDomainPath2);
		
		IntraDomainPath intraDomainPath3 = new IntraDomainPath(interDomainPath, 
				new PathEdge("domain-3", "node-3-1", "port-3-1-1"), 
				new PathEdge("domain-3", "node-3-2", "port-3-2-2"), 
				ConnectionType.POINT_TO_POINT, 
				new TrafficClassifier("10.0.0.1", "10.0.1.1", null, 0),
				new TrafficProfile(20),
				PathStatus.REQUESTED, 
				"domain-3", 
				"path-3-1");
		
		intraDomainPathRepository.saveAndFlush(intraDomainPath3);
	}
	
	@Test
	public void checkDb() {
		Optional<InterDomainPath> interDomainPath = interDomainPathRepository.findByInterDomainPathId("inter-01");
		assertTrue(interDomainPath.isPresent());
		assertTrue(interDomainPath.get().getIntraDomainPaths().size() == 3);
		
		List<IntraDomainPath> intraPaths = intraDomainPathRepository.findByIdpInterDomainPathId("inter-01");
		assertTrue(intraPaths.size() == 3);
	}
	
	@Test
	public void checkAlgorithm() {
		SimpleWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		String v1 = "v1";
		String v2 = "v2";
		String v3 = "v3";
		String v4 = "v4";
		
		graph.addVertex(v1);
		graph.addVertex(v2);
		graph.addVertex(v3);
		graph.addVertex(v4);
		
		graph.addEdge(v1, v2);
		graph.setEdgeWeight(graph.getEdge(v1, v2), 100);
		
		graph.addEdge(v1,v3);
		graph.setEdgeWeight(graph.getEdge(v1, v3), 20);
		
		graph.addEdge(v3,v2);
		graph.setEdgeWeight(graph.getEdge(v3, v2), 30);
		
		graph.addEdge(v1,v4);
		graph.setEdgeWeight(graph.getEdge(v1, v4), 50);
		
		graph.addEdge(v4,v2);
		graph.setEdgeWeight(graph.getEdge(v4, v2), 60);
		
		System.out.println(graph.toString());
		
		DijkstraShortestPath<String, DefaultWeightedEdge> spfAlgorithm = new DijkstraShortestPath<>(graph);
		
		GraphPath<String, DefaultWeightedEdge> path = spfAlgorithm.getPath(v1, v2);
		
		System.out.println(path.toString());
		
		System.out.println("Path weight: " + spfAlgorithm.getPathWeight(v1, v2));
	}
	
	
	@After
	public void clean() {
		cleanDb();
	}
	
	private void cleanDb() {
		interDomainPathRepository.deleteAll();
		intraDomainPathRepository.deleteAll();
		domainsRepository.deleteAll();
		nodeRepository.deleteAll();
	}

}
