'use strict';

const React = require('react')
const ReactDOM = require('react-dom')

const SpinnakerSettings = require('./SpinnakerSettings')
const Settings = require('./Settings')
const Modules = require('./Modules')

class Application extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			api: 'spinnaker.api',
			org: 'spinnaker.org',
			space: 'spinnaker.space',
			email: 'spinnaker.email',
			password: 'spinnaker.password',
			services: 'spring.cloud.deployer.cloudfoundry.services',
			accountName: 'cf.account.name',
			accountPassword: 'cf.account.password',
			repoUsername: 'cf.repo.username',
			repoPassword: 'cf.repo.password',
			springConfigLocation: 'spring.config.location',
			domain: 'deck.domain',
			primaryAccount: 'deck.primaryAccount',
			primaryAccounts: 'deck.primaryAccounts',
			active: 'settings',
			slackEnabled: 'slack.enabled',
			slackToken: 'slack.token',
			jenkinsEnabled: 'jenkins.enabled',
			jenkinsName: 'services.jenkins.defaultMaster.name',
			jenkinsUrl: 'services.jenkins.defaultMaster.baseUrl',
			jenkinsUsername: 'services.jenkins.defaultMaster.username',
			jenkinsPassword: 'services.jenkins.defaultMaster.password'
		}
		this.updateSetting = this.updateSetting.bind(this)
		this.handleSettings = this.handleSettings.bind(this)
		this.handleStatus = this.handleStatus.bind(this)
		this.tabStatus = this.tabStatus.bind(this)
		this.settingsStatus = this.settingsStatus.bind(this)
		this.isActive = this.isActive.bind(this)
	}

	updateSetting(key, value) {
		let newState = {}
		newState[key] = value
		if (key === this.state.api) {
			newState['providers.cf.primaryCredentials.api'] = value
		}
		if (key === this.state.org) {
			newState['providers.cf.defaultOrg'] = value
		}
		if (key === this.state.space) {
			newState['providers.cf.defaultSpace'] = value
		}
		if (key === this.state.email) {
			newState[this.state.accountName] = value
		}
		if (key === this.state.password) {
			newState[this.state.accountPassword] = value
		}
		if (key === this.state.primaryAccount) {
			newState[this.state.primaryAccounts] = value
		}

		this.setState(newState)
	}

	handleSettings(e) {
		e.preventDefault()
		this.setState({active: 'settings'})
	}

	handleStatus(e) {
		e.preventDefault()
		this.setState({active: 'status'})
	}

	isActive(tab) {
		return ((this.state.active === tab) ? ' active' : '')
	}

	tabStatus(tab) {
		return 'tabs__item' + this.isActive(tab)
	}

	settingsStatus(tab) {
		return 'content wrapper' + this.isActive(tab)
	}

	render() {
		return (
			<div>
				<section className="page-header box box--tiny">
					<ul className="tabs">
						<li className={this.tabStatus('settings')} onClick={this.handleSettings}>
							<a id="settings-link" className="tabs__link">Settings</a>
						</li>
						<li className={this.tabStatus('status')} onClick={this.handleStatus}>
							<a id="status-link" className="tabs__link">Status</a>
						</li>
					</ul>
				</section>

				<section className="box box--tiny content__container">
					<div id="settings" className={this.settingsStatus('settings')}>
						<h1>Installation Settings</h1>
						<SpinnakerSettings updateSetting={this.updateSetting}
										   refresh={this.refresh}
										   settings={this.state}/>
						<h1>Spinnaker Settings</h1>
						<Settings updateSetting={this.updateSetting}
								  settings={this.state} />
					</div>

					<div id="status" className={this.settingsStatus('status')}>
						<h1>Spinnaker Status</h1>
						<Modules settings={this.state} />
					</div>
				</section>
			</div>
		)
	}
}

ReactDOM.render(
	<Application />,
	document.getElementById('app')
)

