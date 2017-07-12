
function readInterDCTopology(topologyId, resId) {
	getInterDCTopology(topologyId, resId, createInterDCTopology);
	//createInterDCTopology(null);
}

function getInterDCTopology(elemId, resId, callback) {
	getNetDomains(elemId, resId, callback);
}

function createInterDCTopology(divId, data, resId) {
//	console.log(JSON.stringify(data, null, 4));

	topology = data;
	
//	console.log(JSON.stringify(topology, null, 4));
	
	var nodes = [];
	var edges = [];
	
	for (var i in topology) {
		var domain = topology[i];
		
//		console.log(JSON.stringify(domain, null, 4));
		
		var domainType = domain['domainType'];
//		console.log('Domain Type: ' + domainType);
		
		/*domainType.indexOf('PLUGIN_OCEANIA') >= 0 || domainType.indexOf('PLUGIN_DUMMY') >= 0*/
		var domainAddr = domain['url'].slice(0, domain['url'].lastIndexOf(':') + 1) + '8181/index.html#/topology';
		console.log(domainAddr);
		if (domain.hasOwnProperty('hosts')) {
//			console.log('Pushing DUMMY DOMAIN... ' + domain['domainId']);
			nodes.push({ group: 'nodes', data: { id: domain['domainId'], href: domainAddr, name: 'DOMAIN - ' + domain['domainId'], label: 'DOMAIN - ' + domain['domainId'], weight: 90, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center oceania-domain'});
		} else { /*if (domainType.indexOf('PLUGIN_JULIUS') >= 0)*/
			nodes.push({ group: 'nodes', data: { id: domain['domainId'], href: domainAddr, name: 'DOMAIN - ' + domain['domainId'], label: 'DOMAIN - ' + domain['domainId'], weight: 90, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center julius-domain'});
		}
		
		/*if (domain.hasOwnProperty('hosts')) {
//			console.log('Pushing Hosts... ' + domain['domainId']);
			var hosts = domain['hosts'];
			
//			console.log(JSON.stringify(hosts, null, 4));
			
			for (var j in hosts) {
				var host = hosts[j];
//				console.log(JSON.stringify(host['nodeId'], null, 4));
				nodes.push({ group: 'nodes', data: { id: host['nodeId'], name: 'HOST - ' + host['nodeId'], label: 'HOST - ' + host['nodeId'], weight: 90, faveColor: '#fff', faveShape: 'rectangle' }, classes: 'bottom-center host'});
				edges.push({ group: 'edges', data: { source: host['nodeId'], target: domain['domainId'], faveColor: '#706f6f', strength: 70 }});
			}
		}*/
		
		var gw_nodes = domain['nodes'];
		for (var k in gw_nodes) {
			var node = gw_nodes[k];
			if (domain['type'] === 'ACCESS') {
				nodes.push({ group: 'nodes', data: { id: node['nodeId'], name: 'SWITCH - ' + node['nodeId'], label: 'SWITCH - ' + node['nodeId'], weight: 90, faveColor: '#fff', faveShape: 'ellipse', position: { x: i * 200, y: i * 100 }}, classes: 'bottom-center switch'});
			} else if (domain['type'] === 'CORE') {
				nodes.push({ group: 'nodes', data: { id: node['nodeId'], name: 'SWITCH - ' + node['nodeId'], label: 'SWITCH - ' + node['nodeId'], weight: 90, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center switch-red'});
			}
			var found = false;
			for (var h in edges) {
				if (edges[h]['data']['source'] === node['links'][0]['peerNodeId'] && edges[h]['data']['target'] === node['nodeId']) {
					found = true;
				}
			}
			if (!found) {
				edges.push({ group: 'edges', data: { source: node['nodeId'], target: node['links'][0]['peerNodeId'], faveColor: '#706f6f', strength: 70 }});
			}
			edges.push({ group: 'edges', data: { source: node['nodeId'], target: domain['domainId'], faveColor: '#706f6f', strength: 70 }});
		}
	}
	
//	console.log(nodes);
//	console.log(edges);
	
	//edges.push({ group: 'edges', data: { source: saps[j]['cpdId'], target: saps[j]['nsVirtualLinkDescId'], faveColor: '#000', strength: 70 }, classes: 'questionable'});
	
	var cy = cytoscape({
		container: document.getElementById('cy'),

		layout: {
			name: 'concentric',
			padding: 10,
			zoom: 0.6,
			userZoomingEnabled: true,
			wheelSensitivity: 0,
			maxZoom: 1.2,
			
			concentric: function( node ) { // returns numeric value for each node, placing higher nodes in levels towards the centre
				if (node.hasClass('julius-domain')) {
					console.log('JULIUS');
					return 100;
				} else if (node.hasClass('switch-red')) {
					console.log('Red Switch');
					return 90;
				} else if (node.hasClass('oceania-domain')) {
					console.log('OCEANIA');
					return 10;
				} else return 50;
			},
		
			levelWidth: function( nodes ) { // the variation of concentric values in each level
				return 1;
			}
		},

		style: cytoscape.stylesheet()
			.selector('node')
				.css({
					'shape': 'data(faveShape)',
					'content': 'data(name)',
					'text-valign': 'center',
					'text-outline-width': 1,
					'text-width': 2,
					'text-outline-color': '#000',
					'background-color': 'data(faveColor)',
					'color': '#000',
					'label': 'data(name)'
				})
			.selector(':selected')
				.css({
					'border-width': 3,
					'border-color': '#333'
				})
			.selector('edge')
				.css({
					'curve-style': 'haystack',
					'haystack-radius': 0,
					//'curve-style': 'bezier',
					'opacity': 0.666,
					'width': 'mapData(strength, 70, 100, 2, 6)',
					'target-arrow-shape': 'circle',
					'source-arrow-shape': 'circle',
					'line-color': 'data(faveColor)',
					'source-arrow-color': 'data(faveColor)',
					'target-arrow-color': 'data(faveColor)'
				})
			.selector('edge.questionable')
				.css({
					'line-style': 'dotted',
					'target-arrow-shape': 'diamond',
					'source-arrow-shape': 'diamond'
				})
			.selector('.oceania-domain')
				.css({
					'background-image': './images/oceania_150.png',
					'width': 150,//'mapData(weight, 40, 80, 20, 60)',
					'height': 150
				})
			.selector('.julius-domain')
				.css({
					'background-image': './images/julius_150.png',
					'width': 150,//'mapData(weight, 40, 80, 20, 60)',
					'height': 150
				})
			.selector('.host')
				.css({
					'background-image': './images/server_150.png',
					'width': 150,//'mapData(weight, 40, 80, 20, 60)',
					'height': 150
				})
			.selector('.switch')
				.css({
					'background-image': './images/switch_70.png',
					'width': 70,//'mapData(weight, 40, 80, 20, 60)',
					'height': 70
				})
			.selector('.switch-red')
				.css({
					'background-image': './images/switch_red_70.png',
					'width': 70,//'mapData(weight, 40, 80, 20, 60)',
					'height': 70
				})
			.selector('.faded')
				.css({
					'opacity': 0.25,
					'text-opacity': 0
				})
			.selector('.top-left')
				.css({
					'text-valign': 'top',
					'text-halign': 'left'
				})
			.selector('.top-right')
				.css({
					'text-valign': 'top',
					'text-halign': 'right'
				})
			.selector('.bottom-center')
				.css({
					'text-valign': 'bottom',
					'text-halign': 'center'
				}),

		elements: {
			nodes: nodes,
			edges: edges
		},

		ready: function(){
			window.cy = this;
		}
	});
	
	cy.on('tap', 'node', function(){
		try { // your browser may block popups
		  window.open( this.data('href') );
		} catch(e){ // fall back on url change
		  window.location.href = this.data('href');
		}
	});
	
}