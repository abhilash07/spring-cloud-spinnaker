'use strict'

const React = require('react')
const ReactDOM = require('react-dom')

const client = require('./client')
const when = require('when')
const follow = require('./follow')

const Module = require('./Module')


class Modules extends React.Component {

	constructor(props) {
		super(props);
		this.state = {modules: {}} // TODO: Split up state between each module
		this.findModules = this.findModules.bind(this)
		this.refresh = this.refresh.bind(this)
		this.deploy = this.deploy.bind(this)
		this.undeploy = this.undeploy.bind(this)
		this.start = this.start.bind(this)
		this.stop = this.stop.bind(this)
		this.getNamespace = this.getNamespace.bind(this)
		this.handleRefreshAll = this.handleRefreshAll.bind(this)
		this.handleDeployAll = this.handleDeployAll.bind(this)
		this.handleUndeployAll = this.handleUndeployAll.bind(this)
		this.handleStartAll = this.handleStartAll.bind(this)
		this.handleStopAll = this.handleStopAll.bind(this)
	}

	findModules() {
		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let root = '/api'

		let credentials = {
			api: api,
			org: org,
			space: space,
			email: email,
			password: password,
			namespace: (namespace !== '' ? namespace : '')
		}

		follow(client, root, ['modules'], credentials).done(response => {
			this.setState({modules: response.entity._embedded.appStatuses.reduce((prev, curr) => {
				prev[curr.deploymentId] = curr
				return prev
			}, {})})
		})
	}

	refresh(moduleDetails) {
		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let credentials = {
			api: api,
			org: org,
			space: space,
			email: email,
			password: password,
			namespace: (namespace !== '' ? namespace : '')
		}
		client({method: 'GET', path: moduleDetails._links.self.href, headers: credentials}).done(response => {
			let newModules = this.state.modules
			newModules[response.entity.deploymentId] = response.entity
			this.setState({modules: newModules})
		})
	}

	deploy(moduleDetails) {
		let data = {}

		if (['clouddriver', 'front50', 'gate', 'igor', 'orca'].find(m => moduleDetails.deploymentId.startsWith(m)) !== undefined) {
			if ([undefined, ''].find(i => i === this.props.settings[this.props.settings.services]) === undefined) { // if not empty
				data[this.props.settings.services] = this.props.settings[this.props.settings.services]
			}
		}

		if (moduleDetails.deploymentId.startsWith('clouddriver')) {
			data[this.props.settings.primaryAccount] = this.props.settings[this.props.settings.primaryAccount]
			data[this.props.settings.accountName] = this.props.settings[this.props.settings.accountName]
			data[this.props.settings.accountPassword] = this.props.settings[this.props.settings.accountPassword]
			data[this.props.settings.repoUsername] = this.props.settings[this.props.settings.repoUsername]
			data[this.props.settings.repoPassword] = this.props.settings[this.props.settings.repoPassword]
			if (this.props.settings[this.props.settings.secondAccount]) {
				data['providers.cf.secondaryCredentials.name'] = this.props.settings['providers.cf.secondaryCredentials.name']
				data['providers.cf.secondaryCredentials.api'] = this.props.settings['providers.cf.secondaryCredentials.api']
				data['providers.cf.secondaryCredentials.console'] = this.props.settings['providers.cf.secondaryCredentials.console']
				data['providers.cf.secondaryCredentials.org'] = this.props.settings['providers.cf.secondaryCredentials.org']
				data['providers.cf.secondaryCredentials.space'] = this.props.settings['providers.cf.secondaryCredentials.space']
				data['extra.profiles'] = 'two'
			}
		}

		if (!moduleDetails.deploymentId.startsWith('deck')) { // If NOT deck...
			data[this.props.settings.springConfigLocation] = this.props.settings[this.props.settings.springConfigLocation]
			Object.keys(this.props.settings).map(key => {
				if (key.startsWith('providers.cf')) {
					data[key] = this.props.settings[key]
				}
			})
		}

		if (moduleDetails.deploymentId.startsWith('deck')) {
			data[this.props.settings.primaryAccount] = this.props.settings[this.props.settings.primaryAccount]
			data[this.props.settings.primaryAccounts] = this.props.settings[this.props.settings.primaryAccounts]
			data['providers.cf.defaultOrg'] = this.props.settings['providers.cf.defaultOrg']
		}

		if (moduleDetails.deploymentId.startsWith('echo')) {
			if (this.props.settings[this.props.settings.slackEnabled]) {
				data[this.props.settings.slackEnabled] = this.props.settings[this.props.settings.slackEnabled]
				data[this.props.settings.slackToken] = this.props.settings[this.props.settings.slackToken]
			}
			if (this.props.settings[this.props.settings.emailEnabled]) {
				data[this.props.settings.emailEnabled] = this.props.settings[this.props.settings.emailEnabled]
				data[this.props.settings.emailFrom] = this.props.settings[this.props.settings.emailFrom]
				data[this.props.settings.emailUsername] = this.props.settings[this.props.settings.emailUsername]
				data[this.props.settings.emailPassword] = this.props.settings[this.props.settings.emailPassword]
				data[this.props.settings.emailSmtpHost] = this.props.settings[this.props.settings.emailSmtpHost]
				data[this.props.settings.emailSmtpPort] = this.props.settings[this.props.settings.emailSmtpPort]
				Object.keys(this.props.settings).map(key => {
					if (key.startsWith('spring.mail.properties')) {
						data[key] = this.props.settings[key]
					}
				})
			}
		}

		if (moduleDetails.deploymentId.startsWith('igor')) {
			if (this.props.settings[this.props.settings.jenkinsEnabled]) {
				data[this.props.settings.jenkinsEnabled] = this.props.settings[this.props.settings.jenkinsEnabled]
				data[this.props.settings.jenkinsName] = this.props.settings[this.props.settings.jenkinsName]
				data[this.props.settings.jenkinsUrl] = this.props.settings[this.props.settings.jenkinsUrl]
				data[this.props.settings.jenkinsUsername] = this.props.settings[this.props.settings.jenkinsUsername]
				data[this.props.settings.jenkinsPassword] = this.props.settings[this.props.settings.jenkinsPassword]
			}
			if (this.props.settings[this.props.settings.travisEnabled]) {
				data[this.props.settings.travisEnabled] = this.props.settings[this.props.settings.travisEnabled]
				data[this.props.settings.travisName] = this.props.settings[this.props.settings.travisName]
				data[this.props.settings.travisUrl] = this.props.settings[this.props.settings.travisUrl]
				data[this.props.settings.travisAddress] = this.props.settings[this.props.settings.travisAddress]
				data[this.props.settings.travisToken] = this.props.settings[this.props.settings.travisToken]
			}
		}

		data[this.props.settings.domain] = this.props.settings[this.props.settings.domain]
		data['namespace'] = this.getNamespace()

		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let headers = {
			api: api,
			org: org,
			space: space,
			email: email,
			password: password,
			namespace: (namespace !== '' ? namespace : ''),
			'Content-Type': 'application/json'
		}

		client({
			method: 'POST',
			path: moduleDetails._links.self.href,
			entity: data,
			headers: headers
		}).done(success => {
			this.refresh(moduleDetails)
		})
	}

	undeploy(moduleDetails) {
		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let headers = {
			api: api,
			org: org,
			space: space,
			email: email,
			password: password,
			namespace: (namespace !== '' ? namespace : '')
		}

		client({method: 'DELETE', path: moduleDetails._links.self.href, headers: headers}).done(success => {
			this.refresh(moduleDetails)
		}, failure => {
			alert('FAILURE: ' + failure.entity.message)
		})
	}

	start(moduleDetails) {
		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let headers = {
			api: api,
			org: org,
			space: space,
			email: email,
			password: password,
			namespace: (namespace !== '' ? namespace : '')
		}

		client({method: 'POST', path: moduleDetails._links.start.href, headers: headers}).done(success => {
			this.refresh(moduleDetails)
		}, failure => {
			alert('FAILURE: ' + failure.entity.message)
		})
	}

	stop(moduleDetails) {
		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let headers = {
			api: api,
			org: org,
			space: space,
			email: email,
			password: password,
			namespace: (namespace !== '' ? namespace : '')
		}

		client({method: 'POST', path: moduleDetails._links.stop.href, headers: headers}).done(success => {
			this.refresh(moduleDetails)
		}, failure => {
			alert('FAILURE: ' + failure.entity.message)
		})
	}

	getNamespace() {
		if (this.props.settings['all.namespace'] !== undefined && this.props.settings['all.namespace'] !== '') {
			return '-' + this.props.settings['all.namespace']
		} else {
			return ''
		}
	}

	handleRefreshAll(e) {
		e.preventDefault()
		Object.keys(this.state.modules).map(key => {
			this.refresh(this.state.modules[key])
		})
	}

	handleDeployAll(e) {
		e.preventDefault()
		Object.keys(this.state.modules).map(key => {
			this.deploy(this.state.modules[key])
		})
	}

	handleUndeployAll(e) {
		e.preventDefault()
		Object.keys(this.state.modules).map(key => {
			this.undeploy(this.state.modules[key])
		})
	}

	handleStartAll(e) {
		e.preventDefault()
		Object.keys(this.state.modules).map(key => {
			this.start(this.state.modules[key])
		})
	}

	handleStopAll(e) {
		e.preventDefault()
		Object.keys(this.state.modules).map(key => {
			this.stop(this.state.modules[key])
		})
	}

	render() {
		let modules = Object.keys(this.state.modules).map(name =>
			<Module key={name}
					details={this.state.modules[name]}
					refresh={this.refresh}
					deploy={this.deploy}
					undeploy={this.undeploy}
					start={this.start}
					stop={this.stop}/>)

		return (
			<table className="table table--cosy table--rows">
				<tbody>
				<tr>
					<td></td><td></td>
					<td><button onClick={this.findModules}>Load</button></td>
					<td></td><td></td><td></td><td></td>
				</tr>
				{modules}
				<tr>
					<td></td><td></td>
					<td><button onClick={this.handleRefreshAll}>Refresh All</button></td>
					<td><button onClick={this.handleDeployAll}>Deploy All</button></td>
					<td><button onClick={this.handleUndeployAll}>Undeploy All</button></td>
					<td><button onClick={this.handleStartAll}>Start All</button></td>
					<td><button onClick={this.handleStopAll}>Stop All</button></td>
				</tr>
				</tbody>
			</table>
		)
	}

}

module.exports = Modules