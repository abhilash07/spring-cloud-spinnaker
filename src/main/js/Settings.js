'use strict'

const React = require('react')
const TextInput = require('./TextInput')
const PasswordInput = require('./PasswordInput')
const CheckboxInput = require('./CheckboxInput')
const VariableInput = require('./VariableInput')

class Settings extends React.Component {

	constructor(props) {
		super(props)
		this.handleChange = this.handleChange.bind(this)
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

	render() {
		return (
			<div>
				<ul className="layout">
					<TextInput label="Redis Service"
							   placeHolder="Name of Redis service to bind to"
							   name={this.props.settings.services}
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
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
					<CheckboxInput label="Slack?"
								   name={this.props.settings.slackEnabled}
								   handleChange={this.handleChange}
								   settings={this.props.settings} />
					{ this.props.settings[this.props.settings.slackEnabled] ?
						<TextInput label="Slack Token (Use Bot not Webhook)"
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
					<TextInput label="Spring Config Location override"
							   placeHolder="List of property file URL overrides for Spinnaker"
							   name='spring.config.location'
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<TextInput label="Domain"
							   placeHolder="Domain of Spinnaker and deployments, e.g. cfapps.io"
							   name='deck.domain'
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
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