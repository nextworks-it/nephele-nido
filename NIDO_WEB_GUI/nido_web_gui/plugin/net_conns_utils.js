
function uploadNetConnFromForm(formIds, resId) {
	var jsonObj = JSON.parse('{}');
	jsonObj['interDomainPathId'] = document.getElementById(formIds[0]).value;
	jsonObj['name'] = document.getElementById(formIds[1]).value;
	jsonObj['connectionType'] = document.getElementById(formIds[2]).value;
	
	jsonObj['pathType'] = 'INTER_DOMAIN';
	
	var src = JSON.parse('{}');
	src['domainId'] = document.getElementById(formIds[3]).value;
	src['nodeId'] = document.getElementById(formIds[4]).value;
	src['portId'] = document.getElementById(formIds[5]).value;
	jsonObj['sourceEndPoint'] = src;
	
	var dst = JSON.parse('{}');
	dst['domainId'] = document.getElementById(formIds[6]).value;
	dst['nodeId'] = document.getElementById(formIds[7]).value;
	dst['portId'] = document.getElementById(formIds[8]).value;
	jsonObj['destinationEndPoint'] = dst;
	
	var classifier = JSON.parse('{}');
	var srcIP = document.getElementById(formIds[9]).value;
	if (srcIP !== null ) {
		classifier['srcIpAddress'] = srcIP;
	}
	var dstIP = document.getElementById(formIds[10]).value;
	if (dstIP !== null) {
		classifier['dstIpAddress'] = dstIP;
	}
	var dstMAC = document.getElementById(formIds[11]).value;
	if (dstMAC !== null) {
		classifier['dstEthAddress'] = dstMAC;
	}
	var vlan = document.getElementById(formIds[12]).value;
	if (vlan !== null) {
		classifier['vlanId'] = vlan;
	}
	jsonObj['trafficClassifier'] = classifier;
	
	var profile = JSON.parse('{}');
	profile['bandwidth'] = document.getElementById(formIds[13]).value;
	jsonObj['trafficProfile'] = profile;
	
	jsonObj['pathStatus'] = document.getElementById(formIds[14]).value;
	
	var json = JSON.stringify(jsonObj, null, 4);
	postNetConn(json, resId, 'Network Connection request have been successfully sent','Error while sending Network Connection request', showResultMessage);	
}

function postNetConn(data, resId, okMsg, errMsg, callback) {
    postJsonToURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/operation/path', data, resId, okMsg, errMsg, callback);
}

function readNetConnections(tableId, resId) {
	getNetConnections(tableId, resId , createNetConnectionsTable);
}

function readNetConnection(tableId, resId) {
	getNetConnection(tableId, resId, createNetConnectionsDetailsTable);
}

function getNetConnections(elemId, resId, callback) {
	getJsonFromURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/operation/paths', elemId, callback, resId, null, null);
}

function getNetConnection(elemId, resId, callback) {
	var param = getURLParameter('connId');
	getJsonFromURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/operation/path/' + param, elemId, callback, resId, null, null);
}

function deleteConnection(elemId, resId) {
	var name = elemId.split('|')[0];
	deleteRequestToURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/operation/path/' + name, resId, "network Connection successfully deleted", "Unable to delete Network Connection", showResultMessage);
}

function createNetConnectionsTable(tableId, data, resId) {
//	console.log("Connections: " + JSON.stringify(data,null,4));
    var table = document.getElementById(tableId);
	if (!table) {
		return;
	}
	if (!data || data.length < 1) {
		console.log('No Network Connections');
		table.innerHTML = '<tr>No Network Connections stored in database</tr>';
		return;
	}
	console.log(JSON.stringify(data, null, 4));
	var btnFlag = true;
	var header = createTableHeaderByValues(['Id', 'Src Edge', 'Dst Edge', 'Classifier', 'Bandwidth', 'Status'], btnFlag, false);
	var cbacks = ['net_conn_details.html?connId=', 'deleteConnection'];
	var names = ['View', 'Delete'];
    var columns = [['interDomainPathId'], ['sourceEndPoint'], ['destinationEndPoint'], ['trafficClassifier'], ['trafficProfile', 'bandwidth'], ['pathStatus']];
	var conts = '<tbody>';
	
	for (var i in data) {
		if (data[i]['pathStatus'] == 'DELETED' || data[i]['pathStatus'] == 'TERMINATING') {
			cbacks = ['net_conn_details.html?connId='];
			names = ['View'];
		}
		conts += createNetConnectionsTableContents(data[i], btnFlag, resId, names, cbacks, columns);
	}
	
	table.innerHTML = header + conts + '</tbody>';
}

function createNetConnectionsTableContents(data, btnFlag, resId, names, cbacks, columns) {
	var btnText = '';
	if (btnFlag) {
		var btnText = createActionButton(data['interDomainPathId'], resId, names, cbacks);
	}
	var text = '<tr>' + btnText;
	
	for (var i in columns) {
		var values = [];
		getValuesFromKeyPath(data, columns[i], values);
		
//		console.log(values);
		
		var subText = '';
		var subTable = '';
		if (values[0] instanceof Array) {
			subTable += '<td><table class="table table-borderless">';
			for (var j in values[0]) {
				subTable += '<tr><td>' + values[0][j] + '</td></tr>';
			}
			subText += subTable + '</table></td>';
		}
		if (values !== null && values[0] !== null) {
			if (typeof(values[0]) === 'object') {
				subTable += '<td><table class="table table-borderless">';
//				console.log(columns[i][0]);
//				console.log(JSON.stringify(values[0], null, 4));
				if (columns[i][0].indexOf('sourceEndPoint') >= 0 || columns[i][0].indexOf('destinationEndPoint') >= 0) {
					subTable += '<tr><td><b>DOMAIN ID: </b>' + values[0]['domainId'] + '</td></tr>';
					subTable += '<tr><td><b>NODE ID: </b>' + values[0]['nodeId'] + '</td></tr>';
					subTable += '<tr><td><b>PORT ID: </b>' + values[0]['portId'] + '</td></tr>';
				}
				if (columns[i][0].indexOf('trafficClassifier') >= 0) {
					if (values[0]['srcIpAddress'] !== null && values[0]['srcIpAddress'] !== '') {
						subTable += '<tr><td><b>SRC IP: </b>' + values[0]['srcIpAddress'] + '</td></tr>';
					}
					if (values[0]['dstIpAddress'] !== null && values[0]['dstIpAddress'] !== '') {
						subTable += '<tr><td><b>DST IP: </b>' + values[0]['dstIpAddress'] + '</td></tr>';
					}
					if (values[0]['dstEthAddress'] !== null && values[0]['dstEthAddress'] !== '') {
						subTable += '<tr><td><b>DST MAC: </b>' + values[0]['dstEthAddress'] + '</td></tr>';
					}
					if (values[0]['vlanId'] !== null && values[0]['vlanId'] !== 0) {
						subTable += '<tr><td><b>VLAN: </b>' + values[0]['vlanId'] + '</td></tr>';
					}
				}
				subText += subTable + '</table></td>';
			} else {
				if (values.length > 1) {
					subTable += '<td><table class="table table-borderless">';
					for (var h in values) {
						subTable += '<tr><td>' + values[h] + '</td></tr>';
					}
					subText += subTable + '</table></td>';
				} else {
					if(columns[i].indexOf('pathStatus') >= 0 && values[0] == 'DELETED') {
						subText += '<td>TERMINATED</td>';
					} else {
						subText += '<td>' + values[0] + '</td>';
					}
				}
			}
		} else {
			subText += '<td></td>';
		}
		text += subText;
	}
	text += '</tr>';
	
	return text;
}

function createNetConnectionsDetailsTable(tableId, data, resId) {

	var title = data['interDomainPathId'] + " - Intra Domain Paths:"
	fillTableTitle(title, 'connTitle');
	
	var table = document.getElementById(tableId);
	if (!table) {
		return;
	}
	if (!data || data.length < 1) {
		console.log('No Network Connection Details');
		table.innerHTML = '<tr>No Network Connection Details stored in database</tr>';
		return;
	}
	console.log(JSON.stringify(data, null, 4));
	var btnFlag = false;
	var header = createTableHeaderByValues(['Id', 'Domain Id', 'Source Edge', 'Destination Edge', 'Status'], btnFlag, false);
	var cbacks = [];
	var names = [];
    var columns = [['internalId'], ['domainId'], ['sourceEndPoint'],['destinationEndPoint'], ['pathStatus']];
	var conts = '<tbody>';
	
	for (var i in data['intraDomainPaths']) {
		conts += createNetConnectionsTableContents(data['intraDomainPaths'][i], btnFlag, resId, names, cbacks, columns);
	}
	
	table.innerHTML = header + conts + '</tbody>';
}