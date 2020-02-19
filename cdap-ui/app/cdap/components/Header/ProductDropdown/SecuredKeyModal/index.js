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
import SecuredKeyInterface from 'components/SecuredKeyInterface';
import AddSecuredKeyModal from 'components/Header/ProductDropdown/SecuredKeyModal/AddSecuredKeyModal';
import T from 'i18n-react';
import 'whatwg-fetch';

require('./SecuredKeyModal.scss');
const PREFIX = 'features.SecuredKeyModal';

export default class SecuredKeyModal extends Component {
  constructor(props) {
    super(props);

    this.toggleModal = this.toggleModal.bind(this);
  }

  toggleModal() {
    this.setState({
      error: null
    });
    this.props.toggle();
  }

  render() {
    return (
      <Modal
        isOpen={this.props.isOpen}
        toggle={this.toggleModal}
        className="secured-key-modal cdap-modal"
        backdrop='static'
      >
        <ModalHeader>
          <span>
            {T.translate(`${PREFIX}.modalHeader`)}
          </span>
          <div
            className="close-section float-xs-right"
            onClick={this.toggleModal}
          >
            <span className="fa fa-times" />
          </div>
        </ModalHeader>
        <ModalBody>
          <SecuredKeyInterface handleClose={this.props.toggle} />
          <AddSecuredKeyModal
            isOpen={this.props.addSecuredKeyModalOpen}
            toggle={this.props.toggelAddSecuredKeyModal}
          />
        </ModalBody>
      </Modal>
    );
  }
}

SecuredKeyModal.propTypes = {
  isOpen: PropTypes.bool,
  toggle: PropTypes.func,
  addSecuredKeyModalOpen: PropTypes.bool,
  toggelAddSecuredKeyModal: PropTypes.bool
};
