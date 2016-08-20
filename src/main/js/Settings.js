'use strict'

const React = require('react')

class Settings extends React.Component {

	constructor(props) {
		super(props)
		this.state = {}
		this.handleChange = this.handleChange.bind(this)
	}

	handleChange(e) {
		if (e.target.type === 'checkbox') {
			this.props.updateSetting(e.target.name, e.target.checked)
		} else {
			e.preventDefault()
			this.props.updateSetting(e.target.name, e.target.value)
		}
	}

	render() {
		let labelLayout = 'layout__item u-1/2-lap-and-up u-1/4-desk'
		let inputLayout = 'layout__item u-1/2-lap-and-up u-1/2-desk'
		let lineItemLayout = 'control-group'
		return (
			<div>
				<ul className="layout">
					<li className={lineItemLayout}>
						<label className={labelLayout}>Redis Service</label>
						<input className={inputLayout} type="text"
							   placeholder="Name of Redis service to bind to"
							   name="spring.cloud.deployer.cloudfoundry.services" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Default Org</label>
						<input className={inputLayout} type="text"
							   placeholder="Primary organization Spinnaker will deploy to"
							   name="providers.cf.defaultOrg" onChange={this.handleChange}
							   value={this.props.settings['providers.cf.defaultOrg']}/>
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Default Space</label>
						<input className={inputLayout} type="text"
							   placeholder="Primary space Spinnaker will deploy to"
							   name="providers.cf.defaultSpace" onChange={this.handleChange}
							   value={this.props.settings['providers.cf.defaultSpace']}/>
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Primary Account API</label>
						<input className={inputLayout} type="text"
							   placeholder="API for Spinnaker to make deployments"
							   name="providers.cf.primaryCredentials.api" onChange={this.handleChange}
							   value={this.props.settings['providers.cf.primaryCredentials.api']} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Primary Account Console</label>
						<input className={inputLayout} type="text"
							   placeholder="App Manager URL for default space"
							   name="providers.cf.primaryCredentials.console" onChange={this.handleChange}
						/>
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Account Name</label>
						<input className={inputLayout} type="text"
							   placeholder="User id for making deployments"
							   name="cf.account.name" onChange={this.handleChange}
							   value={this.props.settings['cf.account.name']}/>
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Account Password</label>
						<input className={inputLayout} type="password"
							   placeholder="Password for making deployments"
							   name="cf.account.password" onChange={this.handleChange}
							   value={this.props.settings['cf.account.password']}/>
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Repository Name/Access Code</label>
						<input className={inputLayout} type="text"
							   placeholder="User/Access code to pull down artifacts"
							   name="cf.repo.username" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Repository Password/Secret Code</label>
						<input className={inputLayout} type="password"
							   placeholder="Password/Secret code to pull down artifacts"
							   name="cf.repo.password" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Jenkins?</label>
						<input className={inputLayout} type="checkbox"
							   name="jenkins.enabled"
							   checked={this.props.settings[this.props.settings.jenkinsEnabled]}
							   onChange={this.handleChange} />
					</li>
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<li className={lineItemLayout}>
							<label className={labelLayout}>Jenkins Name</label>
							<input className={inputLayout} type="text"
								   placeholder="Name of your instance of Jenkins"
								   name={this.props.settings.jenkinsName} onChange={this.handleChange} />
						</li>
						: null
					}
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<li className={lineItemLayout}>
							<label className={labelLayout}>Jenkins Base URL</label>
							<input className={inputLayout} type="text"
								   placeholder="URL of your Jenkins server"
								   name={this.props.settings.jenkinsUrl} onChange={this.handleChange} />
						</li>
						: null
					}
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<li className={lineItemLayout}>
							<label className={labelLayout}>Jenkins Username</label>
							<input className={inputLayout} type="text"
								   placeholder="Jenkins username"
								   name={this.props.settings.jenkinsUsername} onChange={this.handleChange} />
						</li>
						: null
					}
					{ this.props.settings[this.props.settings.jenkinsEnabled] ?
						<li className={lineItemLayout}>
							<label className={labelLayout}>Jenkins Password</label>
							<input className={inputLayout} type="password"
								   placeholder="Jenkins password"
								   name={this.props.settings.jenkinsPassword} onChange={this.handleChange} />
						</li>
						: null
					}
					<li className={lineItemLayout}>
						<label className={labelLayout}>Slack?</label>
						<input className={inputLayout} type="checkbox"
							   name={this.props.settings.slackEnabled}
							   checked={this.props.settings[this.props.settings.slackEnabled]}
							   onChange={this.handleChange} />
					</li>
					{ this.props.settings[this.props.settings.slackEnabled] ?
						<li className={lineItemLayout}>
							<label className={labelLayout}>Slack Token (Use Bot not Webhook)</label>
							<input className={inputLayout} type="text"
								   placeholder="Bot token value (NOT Webhook token)"
								   name={this.props.settings.slackToken} onChange={this.handleChange}/>
						</li>
						: null
					}
					<li className={lineItemLayout}>
						<label className={labelLayout}>Spring Config Location override</label>
						<input className={inputLayout} type="text"
							   placeholder="List of property file URL overrides for Spinnaker"
							   name="spring.config.location" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Domain</label>
						<input className={inputLayout} type="text"
							   placeholder="Domain of Spinnaker and deployments, e.g. cfapps.io"
							   name="deck.domain" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Primary Account Name</label>
						<input className={inputLayout} type="text"
							   placeholder="Name of the primary account (e.g. prod)"
							   name="deck.primaryAccount" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>All Account Names (separated by commas)</label>
						<input className={inputLayout} type="text"
							   placeholder="Listing of all accounts (e.g. prod,staging,dev)"
							   name="deck.primaryAccounts" onChange={this.handleChange}
							   value={this.props.settings['deck.primaryAccounts']}/>
					</li>
				</ul>
			</div>
		)
	}

}

module.exports = Settings