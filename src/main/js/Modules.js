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
		this.getNamespace = this.getNamespace.bind(this)
		this.handleRefreshAll = this.handleRefreshAll.bind(this)
		this.handleDeployAll = this.handleDeployAll.bind(this)
		this.handleUndeployAll = this.handleUndeployAll.bind(this)
	}

	findModules() {
		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]
		let namespace = this.getNamespace()

		let root = '/api?api=' + api + '&org=' + org + '&space=' + space + '&email=' + email + '&password=' + password
			+ (namespace !== '' ? '&namespace=' + namespace : '')

		follow(client, root, ['modules']).done(response => {
			this.setState({modules: response.entity._embedded.appStatuses.reduce((prev, curr) => {
				prev[curr.deploymentId] = curr
				return prev
			}, {})})
		})
	}

	refresh(moduleDetails) {
		client({method: 'GET', path: moduleDetails._links.self.href}).done(response => {
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
		}

		data[this.props.settings.domain] = this.props.settings[this.props.settings.domain]
		data['namespace'] = this.getNamespace()

		client({
			method: 'POST',
			path: moduleDetails._links.self.href,
			entity: data,
			headers: {'Content-Type': 'application/json'}}).done(success => {

			this.refresh(moduleDetails)
		})
	}

	undeploy(moduleDetails) {
		client({method: 'DELETE', path: moduleDetails._links.self.href}).done(success => {
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

	render() {
		let modules = Object.keys(this.state.modules).map(name =>
			<Module key={name}
					details={this.state.modules[name]}
					refresh={this.refresh}
					deploy={this.deploy}
					undeploy={this.undeploy} />)

		return (
			<table className="table table--cosy table--rows">
				<tbody>
				<tr>
					<td></td><td></td>
					<td><button onClick={this.findModules}>Load</button></td>
					<td></td><td></td>
				</tr>
				{modules}
				<tr>
					<td></td><td></td>
					<td><button onClick={this.handleRefreshAll}>Refresh All</button></td>
					<td><button onClick={this.handleDeployAll}>Deploy All</button></td>
					<td><button onClick={this.handleUndeployAll}>Undeploy All</button></td>
				</tr>
				</tbody>
			</table>
		)
	}

}

module.exports = Modules