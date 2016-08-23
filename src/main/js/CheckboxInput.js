'use strict';

const React = require('react')

class CheckboxInput extends React.Component {

	constructor(props) {
		super(props)
	}

	render() {
		let labelLayout = 'layout__item u-1/2-lap-and-up u-1/4-desk'
		let inputLayout = 'layout__item u-1/2-lap-and-up u-1/2-desk'
		let lineItemLayout = 'control-group'

		return (<li className={lineItemLayout}>
				<label className={labelLayout}>{this.props.label}</label>
				<input className={inputLayout} type="checkbox"
					   name={this.props.name}
					   checked={this.props.settings[this.props.name]}
					   onChange={this.props.handleChange} />
			</li>
		)
	}

}

module.exports = CheckboxInput