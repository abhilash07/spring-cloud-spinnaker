'use strict';

const React = require('react')
const ReactDOM = require('react-dom')

class DynamicLabelInput extends React.Component {

	constructor(props) {
		super(props)
		this.handleKeyChange = this.handleKeyChange.bind(this)
		this.handleValueChange = this.handleValueChange.bind(this)
		this.delete = this.delete.bind(this)
		this.moveCaretAtEnd = this.moveCaretAtEnd.bind(this)
	}

	handleKeyChange(e) {
		e.preventDefault()
		this.props.removeEntry(this.props.prefix + this.props.label)
		this.props.updateEntry(this.props.prefix + e.target.value, this.props.value)
		this.props.updateEntry('<<lastfield>> of ' + this.props.prefix, this.props.prefix + e.target.value)
	}

	handleValueChange(e) {
		e.preventDefault()
		this.props.updateEntry(this.props.prefix + this.props.label, e.target.value)
	}

	delete(e) {
		e.preventDefault()
		this.props.removeEntry(this.props.prefix + this.props.label)
	}

	moveCaretAtEnd(e) {
		var temp_value = e.target.value
		e.target.value = ''
		e.target.value = temp_value
	}

	componentDidMount() {
		if (this.props.settings['<<lastfield>> of ' + this.props.prefix] === this.props.prefix + this.props.label) {
			ReactDOM.findDOMNode(this.refs.thisLabel).focus()
		}
	}

	render() {
		return (
			<li className='control-group'>
				<label className="layout__item u-1/2-palm u-1/1-lap-and-up u-1/4-desk">
					{this.props.prefix}
				</label>
				<input className='layout__item u-1/2-palm u-1/2-lap-and-up u-1/4-desk' ref="thisLabel" type="text"
					   value={this.props.label}
					   onChange={this.handleKeyChange}
					   value={this.props.label}
					   onFocus={this.moveCaretAtEnd} />
				<input className='layout__item u-1/1-palm u-4/10-lap-and-up u-4/10-desk' type="text"
					   name={this.props.name}
					   onChange={this.handleValueChange}
					   value={this.props.settings[this.props.prefix + this.props.label]} />
				{ !this.props.last
					?
					<button className="layout__item u-1/10-desk" onClick={this.delete}>Remove</button>
					:
					null
				}
			</li>
		)
	}

}

module.exports = DynamicLabelInput