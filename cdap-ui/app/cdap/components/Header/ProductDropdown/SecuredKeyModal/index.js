/*
 * Copyright © 2017 Cask Data, Inc.
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
import { Modal, ModalHeader, ModalBody } from 'reactstrap';
import AddSecuredKeyModal from 'components/Header/ProductDropdown/SecuredKeyModal/AddSecuredKeyModal';
import T from 'i18n-react';
import 'whatwg-fetch';

require('./SecuredKeyModal.scss');

export default class SecuredKeyModal extends Component {
  constructor(props) {
    super(props);

    this.state = {
      addSecuredKeyModalOpen:false,
      error: null
    };

    this.toggleModal = this.toggleModal.bind(this);
    this.toggelAddSecuredKeyModal = this.toggelAddSecuredKeyModal.bind(this);
  }

  toggleModal() {
    this.setState({
      error: null
    });
    this.props.toggle();
  }

  toggelAddSecuredKeyModal() {
    this.setState({
      addSecuredKeyModalOpen: !this.state.addSecuredKeyModalOpen
    });
  }

  render() {
    return (
      <Modal
        isOpen={this.props.isOpen}
        toggle={this.toggleModal}
        size="md"
        className="secured-key-modal cdap-modal"
        backdrop='static'
      >
        <ModalHeader>
          <span>
            {T.translate('features.SecuredKeyModal.modalHeader')}
          </span>
          <div
            className="close-section float-xs-right"
            onClick={this.toggleModal}
          >
            <span className="fa fa-times" />
          </div>
        </ModalHeader>
        <ModalBody>
          <button className="btn"
            onClick={this.toggelAddSecuredKeyModal.bind(this)}>
            Add Secured Key
          </button>
          <AddSecuredKeyModal
            isOpen={this.state.addSecuredKeyModalOpen}
            toggle={this.toggelAddSecuredKeyModal}
          />
        </ModalBody>
      </Modal>
    );
  }
}

SecuredKeyModal.propTypes = {
  isOpen: PropTypes.bool,
  toggle: PropTypes.func
};
