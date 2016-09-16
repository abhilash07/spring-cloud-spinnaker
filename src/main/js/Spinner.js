'use strict';

const React = require('react')

import { Circle } from 'better-react-spinkit'

class Spinner extends React.Component {

	constructor(props) {
		super(props)
	}

	render() {
		return (<li className='control-group'>
			<label className='layout__item u-1/2-lap-and-up u-1/4-desk'></label>
			<span className='layout__item u-1/2-lap-and-up u-3/4-desk' >
				<Circle />
			</span>
		</li>)
	}
}

module.exports = Spinner
