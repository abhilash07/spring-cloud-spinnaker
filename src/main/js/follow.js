'use strict';

function follow(api, rootPath, relArray, headers) {

	var root

	if (headers) {
		root = api({
			method: 'GET',
			path: rootPath,
			headers: headers
		});
	} else {
		root = api({
			method: 'GET',
			path: rootPath
		});
	}

	return relArray.reduce(function(root, arrayItem) {
		var rel = typeof arrayItem === 'string' ? arrayItem : arrayItem.rel;
		return traverseNext(root, rel, arrayItem, headers);
	}, root);

	function traverseNext (root, rel, arrayItem, headers) {
		return root.then(function (response) {
			if (hasEmbeddedRel(response.entity, rel)) {
				return response.entity._embedded[rel];
			}

			if(!response.entity._links) {
				return [];
			}

			if (typeof arrayItem === 'string') {
				if (headers) {
					return api({
						method: 'GET',
						path: response.entity._links[rel].href,
						headers: headers
					});
				} else {
					return api({
						method: 'GET',
						path: response.entity._links[rel].href
					});
				}
			} else {
				if (headers) {
					return api({
						method: 'GET',
						path: response.entity._links[rel].href,
						params: arrayItem.params,
						headers: headers
					});
				} else {
					return api({
						method: 'GET',
						path: response.entity._links[rel].href,
						params: arrayItem.params
					});
				}
			}
		});
	}

	function hasEmbeddedRel (entity, rel) {
		return entity._embedded && entity._embedded.hasOwnProperty(rel);
	}
};

module.exports = follow;