'use strict'

const React = require('react')

const TextInput = require('./TextInput')
const PasswordInput = require('./PasswordInput')
const CheckboxInput = require('./CheckboxInput')
const VariableInput = require('./VariableInput')
const DropdownInput = require('./DropdownInput')
const Spinner = require('./Spinner')

const client = require('./client')
const follow = require('./follow')

class Settings extends React.Component {

	constructor(props) {
		super(props)
		this.handleChange = this.handleChange.bind(this)
		this.toggleOff = this.toggleOff.bind(this)
		this.toggleOn = this.toggleOn.bind(this)
		this.loadRedisServices = this.loadRedisServices.bind(this)
		this.listRedisServices = this.listRedisServices.bind(this)
		this.currentRedisInstanceIsListed = this.currentRedisInstanceIsListed.bind(this)
		this.loadDomains = this.loadDomains.bind(this)
		this.listDomains = this.listDomains.bind(this)
	}

	/**
	 * React.js needs checkbox events to propagate in order to display properly. Otherwise,
	 * don't propagate and handle here.
	 * @param e
	 */
	handleChange(e) {
		if (e.target.type === 'checkbox') {
			this.props.updateSetting(e.target.name, e.target.checked)
		} else {
			e.preventDefault()
			this.props.updateSetting(e.target.name, e.target.value)
		}
	}

	toggleOff(e) {
		e.preventDefault()
		this.props.updateSetting(e.target.name, false)
	}

	toggleOn(e) {
		e.preventDefault()
		this.props.updateSetting(e.target.name, true)
		this.loadRedisServices()
	}

	loadRedisServices() {

		this.props.updateSetting('redisLoading', true)

		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]

		let root = '/api?api=' + api + '&org=' + org + '&space=' + space + '&email=' + email + '&password=' + password

		follow(client, root, [{rel: 'services', params: {serviceType: 'redis'}}]).done(response => {
			this.props.updateSetting('redisLoading', false)

			let redisInstanceNames = response.entity._embedded.serviceInstances.map(serviceInstance => serviceInstance.name);

			/**
			 * Hold onto the list of known redis instances to support the selection widget.
			 */
			this.props.updateSetting(this.props.settings.redisInstances, redisInstanceNames)

			/**
			 * If the fetched list does NOT contain an already selected instance, update it
			 */
			if (redisInstanceNames.length > 0 && !this.currentRedisInstanceIsListed(redisInstanceNames)) {
				this.props.updateSetting(this.props.settings.services, redisInstanceNames[0])
			}
		}, error => {
			this.props.updateSetting('redisLoading', false)

			this.props.updateSetting(this.props.settings.redisInstances, [])
			this.props.updateSetting(this.props.settings.services, '')
		})
	}

	listRedisServices() {
		return this.props.settings[this.props.settings.redisInstances]
	}

	currentRedisInstanceIsListed(redisInstanceNames) {
		return redisInstanceNames.indexOf(this.props.settings[this.props.settings.services]) >= 0;
	}

	loadDomains(e) {
		e.preventDefault()
		this.props.updateSetting('domainsLoading', true)

		let api = this.props.settings[this.props.settings.api]
		let org = this.props.settings[this.props.settings.org]
		let space = this.props.settings[this.props.settings.space]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]

		let root = '/api?api=' + api + '&org=' + org + '&space=' + space + '&email=' + email + '&password=' + password

		follow(client, root, ['domains']).done(response => {
			this.props.updateSetting('domainsLoading', false)

			let domainNames = response.entity._embedded.domains.map(domain => domain.name);

			/**
			 * Hold onto the list of known domains to support the selection widget.
			 */
			this.props.updateSetting(this.props.settings.domains, domainNames)

			if (domainNames.length > 0) {
				this.props.updateSetting(this.props.settings.domain, domainNames[0])
			}
		}, error => {
			this.props.updateSetting('domainsLoading', false)

			this.props.updateSetting(this.props.settings.domains, [])
			this.props.updateSetting(this.props.settings.domain, '')
		})
	}

	listDomains() {
		return this.props.settings[this.props.settings.domains]
	}

	render() {
		return (
			<div>
				<ul className="layout">
					{ this.props.settings[this.props.settings.pickRedisFromDropdown] ?
						this.props.settings.redisLoading ?
							<Spinner />
							:
							<DropdownInput label="Redis Service"
										   name={this.props.settings.services}
										   handleChange={this.handleChange}
										   data={this.listRedisServices}
										   settings={this.props.settings}/>
						:
						<TextInput label="Redis Service"
									 placeHolder="Name of Redis service to bind to"
									 name={this.props.settings.services}
									 handleChange={this.handleChange}
									 settings={this.props.settings}/>
					}
					{ this.props.settings[this.props.settings.pickRedisFromDropdown] ?
						<li className='control-group'>
							<label className='layout__item u-1/2-lap-and-up u-1/4-desk'></label>
							<button className='layout__item u-1/2-lap-and-up u-3/4-desk'
									name={this.props.settings.pickRedisFromDropdown}
									onClick={this.toggleOff}>Text Entry</button>
						</li>
						:
						<li className='control-group'>
							<label className='layout__item u-1/2-lap-and-up u-1/4-desk'></label>
							<button className='layout__item u-1/2-lap-and-up u-3/4-desk'
									name={this.props.settings.pickRedisFromDropdown}
									onClick={this.toggleOn}>Pick from a list</button>
						</li>
					}
					<TextInput label="Default Org"
							   placeHolder="Primary organization Spinnaker will deploy to"
							   name="providers.cf.defaultOrg"
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<TextInput label="Default Space"
							   placeHolder="Primary space Spinnaker will deploy to"
							   name="providers.cf.defaultSpace"
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<TextInput label="Primary Account API"
							   placeHolder="API for Spinnaker to make deployments"
							   name="providers.cf.primaryCredentials.api"
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<TextInput label="Primary Account Console"
							   placeHolder="e.g. https://console.run.pivotal.io or https://apps.example.com"
							   name="providers.cf.primaryCredentials.console"
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<TextInput label="Account Name"
							   placeHolder="User id for making deployments"
							   name={this.props.settings.accountName}
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<PasswordInput label="Account Password"
								   placeHolder="Password for making deployments"
								   name={this.props.settings.accountPassword}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					<TextInput label="Repository Name/Access Code"
							   placeHolder="User/Access code to pull down artifacts"
							   name={this.props.settings.repoUsername}
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<PasswordInput label="Repository Password/Secret Code"
								   placeHolder="Password/Secret code to pull down artifact"
								   name={this.props.settings.repoPassword}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					<CheckboxInput label="Jenkins?"
								   name={this.props.settings.jenkinsEnabled}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<TextInput label="Jenkins Name"
								   placeHolder="Name of your instance of Jenkins"
								   name={this.props.settings.jenkinsName}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<TextInput label="Jenkins Base URL"
								   placeHolder="URL of your Jenkins server"
								   name={this.props.settings.jenkinsUrl}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<TextInput label="Jenkins Username"
								   placeHolder="Jenkins username"
								   name={this.props.settings.jenkinsUsername}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<PasswordInput label="Jenkins Password"
									   placeHolder="Jenkins password"
									   name={this.props.settings.jenkinsPassword}
									   handleChange={this.handleChange}
									   settings={this.props.settings} />
						: null
					}
					<CheckboxInput label="Travis?"
								   name={this.props.settings.travisEnabled}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					{ this.props.settings[this.props.settings.travisEnabled] ?
						<TextInput label="Travis Name"
								   placeHolder="Name of your instance of Travis"
								   name={this.props.settings.travisName}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.travisEnabled] ?
						<TextInput label="Travis Base URL"
								   placeHolder="URL of your Travis server, e.g. https://travis-ci.org"
								   name={this.props.settings.travisUrl}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.travisEnabled] ?
						<TextInput label="Travis API URL"
								   placeHolder="Travis API URL, e.g. https://api.travis-ci.org"
								   name={this.props.settings.travisAddress}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.travisEnabled] ?
						<PasswordInput label={["Travis ", <a href="https://github.com/settings/tokens" target="_blank">GitHub Personal Access Token</a>]}
									   placeHolder="Travis GitHub Personal Access Token"
									   name={this.props.settings.travisToken}
									   handleChange={this.handleChange}
									   settings={this.props.settings} />
						: null
					}
					<CheckboxInput label="Slack?"
								   name={this.props.settings.slackEnabled}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					{ this.props.settings[this.props.settings.slackEnabled] ?
						<TextInput label={["Slack Token (Use ", <a href="https://my.slack.com/services/new/bot" target="_blank">Bot</a>, " not Webhook)"]}
								   placeHolder="Bot token value (NOT Webhook token)"
								   name={this.props.settings.slackToken}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					<CheckboxInput label="Email?"
								   name={this.props.settings.emailEnabled}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					{ this.props.settings[this.props.settings.emailEnabled] ?
						<TextInput label="Email From"
								   placeHolder="Your friendly neighorhood Spinnaker <spinnaker@example.com>"
								   name={this.props.settings.emailFrom}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.emailEnabled] ?
						<TextInput label="Email Username"
								   placeHolder="spinnaker@example.com"
								   name={this.props.settings.emailUsername}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.emailEnabled] ?
						<PasswordInput label="Email Password"
									   placeHolder="Your super secret password for this email account"
									   name={this.props.settings.emailPassword}
									   handleChange={this.handleChange}
									   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.emailEnabled] ?
						<TextInput label="SMTP Host"
								   placeHolder="smtp.gmail.com"
								   name={this.props.settings.emailSmtpHost}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.emailEnabled] ?
						<TextInput label="SMTP Port"
								   placeHolder="587"
								   name={this.props.settings.emailSmtpPort}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
						: null
					}
					{ this.props.settings[this.props.settings.emailEnabled] ?
						<VariableInput
							prefix="spring.mail.properties."
							removeEntry={this.props.removeEntry}
							updateSetting={this.props.updateSetting}
							settings={this.props.settings} />
						: null
					}
					<li className='control-group'>
						<label className='layout__item u-1/2-lap-and-up u-1/4-desk'></label>
						<button className='layout__item u-1/2-lap-and-up u-3/4-desk'
								onClick={this.loadDomains}>Refresh list of domains</button>
					</li>
					{this.props.settings.domainsLoading ?
						<Spinner />
						:
						<DropdownInput label="Domain"
									   name={this.props.settings.domain}
									   handleChange={this.handleChange}
									   data={this.listDomains}
									   settings={this.props.settings}/>
					}
					<TextInput label="Primary Account Name"
							   placeHolder="Name of the primary account (e.g. prod)"
							   name='deck.primaryAccount'
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<TextInput label="All Account Names (separated by commas)"
							   placeHolder="Listing of all accounts (e.g. prod,staging,dev)"
							   name='deck.primaryAccounts'
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
				</ul>
				<br/>
				<br/>
				<br/>
				<br/>
				<br/>
			</div>
		)
	}

}

module.exports = Settings
