'use strict'

const React = require('react')

const TextInput = require('./TextInput')
const PasswordInput = require('./PasswordInput')
const DropdownInput = require('./DropdownInput')
const Spinner = require('./Spinner')

const client = require('./client')
const follow = require('./follow')


class SpinnakerSettings extends React.Component {

	constructor(props) {
		super(props)
		this.state = {}
		this.handleChange = this.handleChange.bind(this)
		this.pws = this.pws.bind(this)
		this.refreshOrgsAndSpaces = this.refreshOrgsAndSpaces.bind(this)
		this.listOrgs = this.listOrgs.bind(this)
		this.listSpaces = this.listSpaces.bind(this)

	}

	handleChange(e) {
		e.preventDefault()
		this.props.updateSetting(e.target.name, e.target.value)

		if (e.target.name === this.props.settings.org) {
			let firstSpace = this.props.settings[this.props.settings.orgsAndSpaces][e.target.value][0]
			this.props.updateSetting(this.props.settings.space, firstSpace)
		}
	}

	pws(e) {
		e.preventDefault()
		this.props.updateSetting(this.props.settings.api, 'https://api.run.pivotal.io')
	}

	refreshOrgsAndSpaces(e) {
		e.preventDefault()

		this.props.updateSetting('orgsAndSpacesLoading', true)

		let api = this.props.settings[this.props.settings.api]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]

		let root = '/api?api=' + api + '&email=' + email + '&password=' + password

		follow(client, root, ['orgs']).done(response => {
			this.props.updateSetting('orgsAndSpacesLoading', false)

			let orgsAndSpaces = response.entity.content

			this.props.updateSetting(this.props.settings.orgsAndSpaces, orgsAndSpaces)

			if (orgsAndSpaces) {
				let firstOrg = Object.keys(orgsAndSpaces)[0]
				let firstSpace = orgsAndSpaces[firstOrg][0]

				this.props.updateSetting(this.props.settings.org, firstOrg)
				this.props.updateSetting(this.props.settings.space, firstSpace)
			}
		}, error => {
			this.props.updateSetting('orgsAndSpacesLoading', false)

			this.props.updateSetting(this.props.settings.orgsAndSpaces, {})
			this.props.updateSetting(this.props.settings.org, '')
			this.props.updateSetting(this.props.settings.space, '')
		})
	}

	listOrgs() {
		return Object.keys(this.props.settings[this.props.settings.orgsAndSpaces])
	}

	listSpaces() {
		if (this.props.settings[this.props.settings.org]) {
			return this.props.settings[this.props.settings.orgsAndSpaces][this.props.settings[this.props.settings.org]]
		} else {
			return []
		}
	}

	render() {

		let pwsButton = <button className="layout__item u-1/16-lap-and-up u-1/16-desk" onClick={this.pws}>PWS</button>

		return (
			<div>
				<ul className="layout">
					<TextInput label="Target API"
							   placeHolder="API to install Spinnaker, e.g. https://api.run.pivotal.io"
							   name={this.props.settings.api}
							   handleChange={this.handleChange}
							   optional={pwsButton}
							   inputLayout="layout__item u-7/16-lap-and-up u-11/16-desk"
							   settings={this.props.settings} />
					<TextInput label="Target Email"
							   placeHolder="Login email to install Spinnaker"
							   name={this.props.settings.email}
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<PasswordInput label="Target Password"
							   placeHolder="Password to install Spinnaker"
							   name={this.props.settings.password}
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
					<li className='control-group'>
						<label className='layout__item u-1/2-lap-and-up u-1/4-desk'></label>
						<button className='layout__item u-1/2-lap-and-up u-3/4-desk'
								onClick={this.refreshOrgsAndSpaces}>Refresh</button>
					</li>
					{ this.props.settings.orgsAndSpacesLoading ?
						<Spinner />
						:
						<span>
							<DropdownInput label="Target Organization"
										   name={this.props.settings.org}
										   handleChange={this.handleChange}
										   data={this.listOrgs}
										   settings={this.props.settings}/>
							<DropdownInput label="Target Space"
										   name={this.props.settings.space}
										   handleChange={this.handleChange}
										   data={this.listSpaces}
										   settings={this.props.settings}/>
						</span>
					}
					<TextInput label="Namespace"
							   placeHolder="Logical name for each module of Spinnaker (e.g. alice)"
							   name="all.namespace"
							   handleChange={this.handleChange}
							   settings={this.props.settings} />
				</ul>
			</div>
		)
	}

}

module.exports = SpinnakerSettings
