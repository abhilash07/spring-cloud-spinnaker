'use strict';

const React = require('react')

class DropdownInput extends React.Component {

	constructor(props) {
		super(props)
		this.props.loadData()
	}

	render() {
		let options = this.props.data().map(option => <option key={option} value={option}>{option}</option>)

		return (<li className='control-group'>
			<label className='layout__item u-1/2-lap-and-up u-1/4-desk'>{this.props.label}</label>
			<select className='layout__item u-1/2-lap-and-up u-3/4-desk'
					name={this.props.name}
					onChange={this.props.handleChange}>
				{options}
			</select>
		</li>)
	}

}

module.exports = DropdownInput