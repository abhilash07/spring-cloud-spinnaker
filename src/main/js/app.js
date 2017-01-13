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
			console: 'providers.cf.primaryCredentials.console',
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
			jenkinsPassword: 'services.jenkins.defaultMaster.password',
			travisEnabled: 'travis.enabled',
			travisName: 'services.travis.defaultMaster.name',
			travisUrl: 'services.travis.defaultMaster.baseUrl',
			travisAddress: 'services.travis.defaultMaster.address',
			travisToken: 'services.travis.defaultMaster.githubToken',
			emailEnabled: 'mail.enabled',
			emailFrom: 'mail.from',
			emailSmtpHost: 'spring.mail.host',
			emailSmtpPort: 'spring.mail.port',
			emailUsername: 'spring.mail.username',
			emailPassword: 'spring.mail.password',
			pickRedisFromDropdown: 'pick.redis.from.downdown',
			redisInstances: 'redis.instances',
			'redis.instances': [],
			orgsAndSpaces: 'orgs.and.spaces',
			'orgs.and.spaces': {},
			redisLoading: false,
			orgsAndSpacesLoading: false,
			domains: 'domain.names',
			'domain.names': [],
			domainsLoading: false,
			secondAccount: 'second.clouddriveraccount.enabled',
			'providers.cf.secondaryCredentials.name': '',
			'providers.cf.secondaryCredentials.api': '',
			'providers.cf.secondaryCredentials.console': '',
			'providers.cf.secondaryCredentials.org': '',
			'providers.cf.secondaryCredentials.space': ''
		}
		this.removeEntry = this.removeEntry.bind(this)
		this.updateSetting = this.updateSetting.bind(this)
		this.handleSettings = this.handleSettings.bind(this)
		this.handleStatus = this.handleStatus.bind(this)
		this.handleArtifacts = this.handleArtifacts.bind(this)
		this.tabStatus = this.tabStatus.bind(this)
		this.settingsStatus = this.settingsStatus.bind(this)
		this.isActive = this.isActive.bind(this)
	}

	removeEntry(key) {
		let newState = this.state
		delete newState[key]
		this.setState(newState)
	}

	updateSetting(key, value) {
		/**
		 * Copy the supplied value into a key.
		 */
		let newState = {}
		newState[key] = value

		/**
		 * Collection of special rules, that when filling out one field, causes another one to be filled out.
		 */

		/**
		 * Copy one field into another, automatically. NOTE: It's one-way.
		 */
		if (key === this.state.api) {
			newState['providers.cf.primaryCredentials.api'] = value
			newState['providers.cf.secondaryCredentials.api'] = value
		}
		if (key === this.state.org) {
			newState['providers.cf.defaultOrg'] = value
			newState['providers.cf.secondaryCredentials.org'] = value
		}
		if (key === this.state.space) {
			newState['providers.cf.defaultSpace'] = value
			newState['providers.cf.secondaryCredentials.space'] = value
		}
		if (key === this.state.email) {
			newState[this.state.accountName] = value
		}
		if (key === this.state.password) {
			newState[this.state.accountPassword] = value
		}
		if (key === this.state.primaryAccount) {
			if (this.state[this.state.secondAccount]) {
				newState[this.state.primaryAccounts] = value + "," + this.state['providers.cf.secondaryCredentials.name']
			} else {
				newState[this.state.primaryAccounts] = value
			}
		}
		if (key === 'providers.cf.secondaryCredentials.name') {
			newState[this.state.primaryAccounts] = this.state[this.state.primaryAccount] + "," + value
		}
		if (key === 'providers.cf.secondaryCredentials.name') {
			newState['providers.cf.secondaryCredentials.space'] = value
		}

		/**
		 * If user enters API for PWS, then pre-load PWS's App Manager URL
		 */
		if (key === this.state.api && value === 'https://api.run.pivotal.io') {
			newState[this.state.console] = 'https://console.run.pivotal.io'
			newState['providers.cf.secondaryCredentials.console'] = 'https://console.run.pivotal.io'
		}

		/**
		 * If user is filling out email username, use it to pre-select some common
		 * SMTP settings
		 */
		if (key === this.state.emailUsername) {
			if (value.endsWith('@gmail.com')) {
				newState[this.state.emailSmtpHost] = 'smtp.gmail.com'
				newState[this.state.emailSmtpPort] = '587'
			} else if (value.endsWith('@yahoo.com')) {
				newState[this.state.emailSmtpHost] = 'smtp.yahoo.com'
				newState[this.state.emailSmtpPort] = '587'
			} else {
				newState[this.state.emailSmtpHost] = ''
				newState[this.state.emailSmtpPort] = ''
			}
		}

		if (key === this.state.emailEnabled) {
			if (value === true) {
				newState['spring.mail.properties.mail.smtp.auth'] = 'true'
				newState['spring.mail.properties.mail.smtp.starttls.enable'] = 'true'
				newState['spring.mail.properties.mail.debug'] = 'true'
			}
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

	handleArtifacts(e) {
		e.preventDefault()
		this.setState({active: 'artifacts'})
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
						<li className={this.tabStatus('artifacts')} onClick={this.handleArtifacts}>
							<a id="artifacts-link" className="tabs__link">Artifacts</a>
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
								  removeEntry={this.removeEntry}
								  settings={this.state} />
					</div>

					<div id="status" className={this.settingsStatus('status')}>
						<h1>Spinnaker Status</h1>
						<Modules settings={this.state} />
					</div>

					<div id="artifacts" className={this.settingsStatus('artifacts')}>
						<h1>Spinnaker Artifacts</h1>
						<textarea value={JSON.stringify(this.state, null, 2)} readOnly="" className="artifacts"/>
						<a href={'data:application/json;charset=utf-8,' + JSON.stringify(this.state, null, 2)} download="spinnaker.json">Download</a>
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

