
function uploadNetDomFromForm(formIds, resId) {
	var jsonObj = JSON.parse('{}');
	jsonObj['domainId'] = document.getElementById(formIds[0]).value;
	jsonObj['domainType'] = document.getElementById(formIds[1]).value;
	jsonObj['url'] = document.getElementById(formIds[2]).value;
	jsonObj['type'] = document.getElementById(formIds[3]).value;
	
	var json = JSON.stringify(jsonObj, null, 4);
	postNetDom(json, resId, 'Network Domain have been successfully uploaded','Error while uploading Network Domain', showResultMessage);
		
}

function postNetDom(data, resId, okMsg, errMsg, callback) {
    postJsonToURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/management/domain/', data, resId, okMsg, errMsg, callback);
}

function readNetDomains(tableId, resId) {
	getNetDomains(tableId, resId , createNetDomainsTable);
}

function readNetDomain(tableId, resId) {
	getNetDomain(tableId, resId, createNetDomainDetailsTable);
}

function getNetDomains(elemId, resId, callback) {
	getJsonFromURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/management/domains/', elemId, callback, resId, null, null);
}

function getNetDomain(elemId, resId, callback) {
	var id = getURLParameter('netDomId');
	getJsonFromURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/management/domain/' + id, elemId, callback, resId, null, null);
}

function deleteNetDom(netDomId, resId) {
	var id = netDomId.split('|')[0];
	deleteRequestToURL('http://' + nfvoAddr + ':' + nfvoPort + '/nido/management/domain/' + id, resId, "Network Domain successfully deleted", "Unable to delete network Domain", showResultMessage);
}

function createNetDomainsTable(tableId, data, resId) {
//	console.log("Domains: " + JSON.stringify(data,null,4));
    var table = document.getElementById(tableId);
	if (!table) {
		return;
	}
	if (!data || data.length < 1) {
		console.log('No Network Domains');
		table.innerHTML = '<tr>No Network Domains stored in database</tr>';
		return;
	}
//  console.log(JSON.stringify(data, null, 4));
	var btnFlag = true;
	var header = createTableHeaderByValues(['Domain Id', 'Plugin Type', 'Url', 'Domain Type'], btnFlag, false);
	var cbacks = ['deleteNetDom'];
	var names = ['Delete'];
    var columns = [['domainId'], ['domainType'], ['url'], ['type']];
	var conts = '<tbody>';
	for (var i in data) {
		conts += createNetDomainsTableContents(data[i], btnFlag, resId, names, cbacks, columns);
	}
	table.innerHTML = header + conts + '<tbody>';
}

function createNetDomainsTableContents(data, btnFlag, resId, names, cbacks, columns) {
	var btnText = '';
	if (btnFlag) {
		var btnText = createActionButton(data['domainId'], resId, names, cbacks);
	}
	var text = '<tr>' + btnText;
	
	for (var i in columns) {
		var values = [];
		getValuesFromKeyPath(data, columns[i], values);
		
//		console.log(values);
		
		var subText = '';
		var subTable = '<td><table class="table table-borderless">';
		if (values[0] instanceof Array) {
			for (var j in values[0]) {
				subTable += '<tr><td>' + values[0][j] + '</td></tr>';
			}
			subText += subTable + '</table></td>';
		} else {
			if (values.length > 1) {
				for (var h in values) {
					subTable += '<tr><td>' + values[h] + '</td></tr>';
				}
				subText += subTable + '</table></td>';
			} else {
				if (columns[i].indexOf('type') >= 0 && values[0] == 'ACCESS') {
					subText += '<td>DC NETWORK</td>';
				} else {
					subText += '<td>' + values[0] + '</td>';
				}
			}
		}
		text += subText;
	}
	text += '</tr>';
	
	return text;
}
