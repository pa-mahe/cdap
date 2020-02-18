import React from 'react';
import PropTypes from 'prop-types';
import SecuredKeyGrid from 'components/SecuredKeyGrid';
require('./SecuredKeyInterface.scss');

export default class SecuredKeyInterface extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    handleClose: PropTypes.func
  }

  render() {
    return (
      <SecuredKeyGrid/>
    );
  }
}
