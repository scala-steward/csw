import React from 'react'
import PropTypes from 'prop-types'
import {TMTAuthContext} from '../TMTAuthContext.jsx'

class RealmRole extends React.Component {
  render() {
    return (
      <TMTAuthContext.Consumer>
        {context => {
          return (
            <div className='card-content'>
              {(context.isAuthenticated && context.tmtAuth.hasRealmRole(this.props.realmRole)) ? this.props.children : null}
            </div>
          )
        }}
      </TMTAuthContext.Consumer>
    )
  }
}

RealmRole.propTypes = {
  realmRole: PropTypes.string.isRequired,
  children: PropTypes.node
}

RealmRole.contextType = TMTAuthContext

export default RealmRole
