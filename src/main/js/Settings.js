'use strict'

const React = require('react')

class Settings extends React.Component {

	constructor(props) {
		super(props)
		this.state = {}
		this.handleChange = this.handleChange.bind(this)
	}

	handleChange(e) {
		e.preventDefault()
		this.props.updateSetting(e.target.name, e.target.value)
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
							   name="spring.cloud.deployer.cloudfoundry.defaults.services" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Default Org</label>
						<input className={inputLayout} type="text"
							   placeholder="Primary organization Spinnaker will deploy to"
							   name="providers.cf.defaultOrg" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Default Space</label>
						<input className={inputLayout} type="text"
							   placeholder="Primary space Spinnaker will deploy to"
							   name="providers.cf.defaultSpace" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Primary Account API</label>
						<input className={inputLayout} type="text"
							   placeholder="API for Spinnaker to make deployments"
							   name="providers.cf.primaryCredentials.api" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Primary Account Console</label>
						<input className={inputLayout} type="text"
							   placeholder="App Manager URL for default space"
							   name="providers.cf.primaryCredentials.console" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Account Name</label>
						<input className={inputLayout} type="text"
							   placeholder="User id for making deployments"
							   name="cf.account.name" onChange={this.handleChange} />
					</li>
					<li className={lineItemLayout}>
						<label className={labelLayout}>Account Password</label>
						<input className={inputLayout} type="password"
							   placeholder="Password for making deployments"
							   name="cf.account.password" onChange={this.handleChange} />
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
							   name="deck.primaryAccounts" onChange={this.handleChange} />
					</li>
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

module.exports = Settings