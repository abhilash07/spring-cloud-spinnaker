'use strict';

const React = require('react')

class PasswordInput extends React.Component {

	constructor(props) {
		super(props)
	}

	render() {
		return (<li className='control-group'>
			<label className='layout__item u-1/2-lap-and-up u-1/4-desk'>{this.props.label}</label>
			<input className='layout__item u-1/2-lap-and-up u-3/4-desk' type="password"
				   placeholder={this.props.placeHolder}
				   name={this.props.name}
				   onChange={this.props.handleChange}
				   value={this.props.settings[this.props.name]} />
		</li>)
	}

}

module.exports = PasswordInput