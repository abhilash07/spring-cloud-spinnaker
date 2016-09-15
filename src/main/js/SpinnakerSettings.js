'use strict'

const React = require('react')

const DropdownInput = require('./DropdownInput')

const client = require('./client')
const follow = require('./follow')


class SpinnakerSettings extends React.Component {

	constructor(props) {
		super(props)
		this.state = {}
		this.handleChange = this.handleChange.bind(this)
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

	refreshOrgsAndSpaces(e) {
		e.preventDefault()
		let api = this.props.settings[this.props.settings.api]
		let email = this.props.settings[this.props.settings.email]
		let password = this.props.settings[this.props.settings.password]

		let root = '/api?api=' + api + '&email=' + email + '&password=' + password

		follow(client, root, ['orgs']).done(response => {

			let orgsAndSpaces = response.entity.content

			console.log(orgsAndSpaces)

			this.props.updateSetting(this.props.settings.orgsAndSpaces, orgsAndSpaces)

			if (orgsAndSpaces) {
				let firstOrg = Object.keys(orgsAndSpaces)[0]
				let firstSpace = orgsAndSpaces[firstOrg][0]

				this.props.updateSetting(this.props.settings.org, firstOrg)
				this.props.updateSetting(this.props.settings.space, firstSpace)
			}
		}, error => {
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
		let labelLayout = 'layout__item u-1/2-lap-and-up u-1/4-desk'
		let inputLayout = 'layout__item u-1/2-lap-and-up u-3/4-desk'
		let lineItemLayout = 'control-group'
		return (
			<div>
				<ul className="layout">
					<li className={lineItemLayout}>
						<label className={labelLayout}>Target API</label>
						<input className={inputLayout} type="text"
							   name="spinnaker.api"
							   placeholder="API to install Spinnaker, e.g. https://api.run.pivotal.io"
							   onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Target Email</label>
						<input className={inputLayout} type="text"
							   name="spinnaker.email"
							   placeholder="Login email to install Spinnaker"
							   onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Target Password</label>
						<input className={inputLayout} type="password"
							   name="spinnaker.password"
							   placeholder="Password to install Spinnaker"
							   onChange={this.handleChange} />
					</li>
					<li className='control-group'>
						<label className='layout__item u-1/2-lap-and-up u-1/4-desk'></label>
						<button className='layout__item u-1/2-lap-and-up u-3/4-desk'
								onClick={this.refreshOrgsAndSpaces}>Refresh</button>
					</li>
					<DropdownInput label="Target Organization"
								   name={this.props.settings.org}
								   handleChange={this.handleChange}
								   data={this.listOrgs}
								   settings={this.props.settings} />
					<DropdownInput label="Target Space"
								   name={this.props.settings.space}
								   handleChange={this.handleChange}
								   data={this.listSpaces}
								   settings={this.props.settings} />
					<li className={lineItemLayout}>
						<label className={labelLayout}>Namespace</label>
						<input className={inputLayout} type="text"
							   placeholder="Logical name for each module of Spinnaker (e.g. alice)"
							   name="all.namespace" onChange={this.handleChange} />
					</li>
				</ul>
			</div>
		)
	}

}

module.exports = SpinnakerSettings
