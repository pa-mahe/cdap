/*
 * Copyright © 2017-2018 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
*/

import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Modal, ModalBody, ModalHeader} from 'reactstrap';

require('./SecuredKeyDataModal.scss');

export default class SecuredKeyDataModal extends Component {
state = {
    title: this.props.title,
    showModal: this.props.showModal || false,
    message: this.props.message,
    };

static propTypes = {
    showModal: PropTypes.bool,
    message: PropTypes.string,
    title: PropTypes.string,
    handleClose: PropTypes.func,
  };

  componentWillReceiveProps(nextProps) {
    let {title, showModal, message} = nextProps;
    if (
        title !== this.state.title ||
        showModal !== this.state.showModal ||
        message !== this.state.message) {
      this.setState({
        title,
        showModal,
        message,
      });
    }
  }

  onClose = () => {
    this.setState({
      showModal: false,
      message: '',
      title: '',
    });
    if (this.props.handleClose) {
      this.props.handleClose();
    }
  };

  render() {
    return (
        <Modal
        isOpen={this.state.showModal}
        toggle={this.onClose.bind(this)}
        className="cdap-modal"
        backdrop='static'
      >
        <ModalHeader>
          <span>
            {this.state.title}
          </span>
          <div
            className="close-section float-xs-right"
            onClick={this.onClose.bind(this)}
          >
            <span className="fa fa-times" />
          </div>
        </ModalHeader>
        <ModalBody>
            <div className = "security-key-data-message">
                {this.state.message}
            </div>
        </ModalBody>
      </Modal>
    );
  }
}
