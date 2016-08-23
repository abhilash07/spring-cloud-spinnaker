'use strict';

const React = require('react')
const TextInput = require('./TextInput')
const DynamicLabelInput = require('./DynamicLabelInput')

class VariableInput extends React.Component {

	constructor(props) {
		super(props)
		this.removeEntry = this.removeEntry.bind(this)
		this.updateEntry = this.updateEntry.bind(this)
	}

	removeEntry(key) {
		this.props.removeEntry(key)
	}

	updateEntry(key, value) {
		this.props.updateSetting(key, value)
	}

	render() {
		let entries = Object.keys(this.props.settings)
			.filter(key => key.startsWith(this.props.prefix))
			.map(key => {
				return {
					key: key,
					label: key.split(this.props.prefix)[1],
					value: this.props.settings[key]
				}
			})
			.map(data => {
				return <DynamicLabelInput key={data.key}
										  prefix={this.props.prefix}
										  label={data.label}
										  value={data.value}
										  removeEntry={this.removeEntry}
										  updateEntry={this.updateEntry}
										  last={false}
										  settings={this.props.settings} />
			})
		let newEntry = <DynamicLabelInput key='blank'
										  prefix={this.props.prefix}
										  label=""
										  value=""
										  removeEntry={this.removeEntry}
										  updateEntry={this.updateEntry}
										  last={true}
										  settings={this.props.settings} />

		return (
			<div>
				{entries}
				{newEntry}

			</div>
		)
	}

}

module.exports = VariableInput