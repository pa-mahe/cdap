import React from 'react';
import PropTypes from 'prop-types';
import OverlayPortal from 'components/OverlayPortal';
require('./FullScreenOverlay.scss');

export default class FullScreenOverlay extends React.PureComponent {
  static propTypes = {
    children: PropTypes.node,
    title: PropTypes.string,
    handleClose: PropTypes.func.isRequired
  }

  render() {
    return (
      <OverlayPortal>
        <div className="wrapper">
          <div className = "top-panel">
            <div className = "overlay-title">{ this.props.title? this.props.title: "" }</div>
            <i className="close fa fa-times" aria-hidden="true" onClick={this.props.handleClose}></i>
          </div>
          <div className="inner">
            {this.props.children}
          </div>
        </div>
      </OverlayPortal>
    );
  }
}
