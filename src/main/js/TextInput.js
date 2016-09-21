'use strict';

const React = require('react')

class TextInput extends React.Component {

	constructor(props) {
		super(props)
	}

	render() {
		let labelLayout = (this.props.labelLayout) ? this.props.labelLayout : 'layout__item u-1/2-lap-and-up u-1/4-desk'
		let inputLayout = (this.props.inputLayout) ? this.props.inputLayout : 'layout__item u-1/2-lap-and-up u-3/4-desk'
		let optional = (this.props.optional) ? this.props.optional : null
		return (<li className='control-group'>
			<label className={labelLayout}>{this.props.label}</label>
			{optional}
			<input className={inputLayout} type="text"
				   placeholder={this.props.placeHolder}
				   name={this.props.name}
				   onChange={this.props.handleChange}
				   value={this.props.settings[this.props.name]} />
		</li>)
	}

}

module.exports = TextInput