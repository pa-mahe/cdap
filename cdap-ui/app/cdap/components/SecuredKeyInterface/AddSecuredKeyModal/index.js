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
import {Modal, ModalHeader, ModalBody, ModalFooter} from 'reactstrap';
import T from 'i18n-react';
import types from 'services/inputValidationTemplates';
import KeyValuePairs from 'components/KeyValuePairs';
import ValidatedInput from 'components/ValidatedInput';
import cloneDeep from 'lodash/cloneDeep';
import uuidV4 from 'uuid/v4';
import NamespaceStore from 'services/NamespaceStore';
import CardActionFeedback, { CARD_ACTION_TYPES } from 'components/CardActionFeedback';
import Datasource from 'services/datasource';
import {objectQuery} from 'services/helpers';

import 'whatwg-fetch';
import { MySecureKeyApi } from 'api/securekey';

require('./AddSecuredKeyModal.scss');

const PREFIX = 'features.SecuredKeyModal.AddSecuredKeyModal';
const LABEL_COL_CLASS = 'col-xs-3 col-form-label text-xs-right';
const INPUT_COL_CLASS = 'col-xs-8';
const DEFAULT_SECURED_KEY_PROPERTIES = {
  'pairs': [{
    'key': '',
    'value': '',
    'validKey': true,
    'validValue': true,
    'uniqueId': uuidV4()
  }]
};
export default class AddSecuredKeyModal extends Component {
  constructor(props) {
    super(props);

    this.state = {
      name: '',
      description: '',
      data:'',
      securedKeyProperties: cloneDeep(DEFAULT_SECURED_KEY_PROPERTIES),
      loading: false,
      result: {
        message: null,
        type: null
      },
      inputs: this.getValidationState(),
      isOpen: this.props.isOpen
    };
    this.dataSrc = new Datasource();

    this.toggleModal = this.toggleModal.bind(this);
    this.onKeyValueChange = this.onKeyValueChange.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.apply = this.apply.bind(this);
  }

  getValidationState() {
    return {
      name: {
        error: '',
        required: true,
        template: 'DEFAULT',
        label: T.translate(`${PREFIX}.name`)
      },
      description: {
        error: '',
        required: true,
        template: 'DEFAULT',
        label: T.translate(`${PREFIX}.description`)
      },
      data: {
        error: '',
        required: true,
        template: 'DEFAULT',
        label: T.translate(`${PREFIX}.data`)
      }
    };
  }

  toggleModal() {
    this.setState({
      name: '',
      description: '',
      data:'',
      securedKeyProperties: cloneDeep(DEFAULT_SECURED_KEY_PROPERTIES),
      loading: false,
      result: {
        message: null,
        type: null
      },
      inputs: this.getValidationState()
    });
    this.props.toggle();
  }

  handleChange(key, e) {
    if (Object.keys(this.state.inputs).includes(key)) {
      // validate input
      const isValid = types[this.state.inputs[key]['template']].validate(e.target.value);
      let errorMsg = '';
      if (e.target.value && !isValid) {
        errorMsg = types[this.state.inputs[key]['template']].getErrorMsg();
      }

      this.setState({
        [key]: e.target.value,
        inputs: {
          ...this.state.inputs,
          [key]: {
            ...this.state.inputs[key],
            'error': errorMsg
          }
        }
      });
    } else {
      this.setState({
        [key]: e.target.value
      });
    }
  }

  onKeyValueChange(securedKeyProperties) {
    this.setState({securedKeyProperties});
  }

  renderSecuredKeyProperties() {
    return (
      <div className="form-group row">
        <label className={LABEL_COL_CLASS}>
          {T.translate(`${PREFIX}.securedKeyProperties`)}
        </label>
        <div className={`${INPUT_COL_CLASS} secured-key-prop-container`}>
          <KeyValuePairs
            keyValues = {this.state.securedKeyProperties}
            onKeyValueChange = {this.onKeyValueChange}
          />
        </div>
      </div>
    );
  }

  isButtonDisabled() {
    return !this.state.name || !this.state.description || !this.state.data;
  }

  getKeyValObject() {
    let keyValArr = this.state.securedKeyProperties ? this.state.securedKeyProperties.pairs : cloneDeep(DEFAULT_SECURED_KEY_PROPERTIES.pairs);
    let keyValObj = {};
    keyValArr.forEach((pair) => {
      if (pair.key.length > 0 && pair.value.length > 0) {
        keyValObj[pair.key] = pair.value;
      }
    });
    return keyValObj;
  }

  apply() {
    let namespace = NamespaceStore.getState().selectedNamespace;
    this.setState({
      loading: true,
      result: {
        message: null,
        type: null
      }
    });

    let requestBody = {
      description: this.state.description,
      data: this.state.data,
      properties: this.getKeyValObject()
    };

    MySecureKeyApi.create({namespace, name:this.state.name}, requestBody)
      .subscribe((response) => {
        let message = objectQuery(response, 'response', 'message') || objectQuery(response, 'response') || T.translate(`${PREFIX}.defaultSuccessMessage`);
          this.setState({
            result: {
              type: CARD_ACTION_TYPES.SUCCESS,
              message: message
            }
          });
      }, (err) => {
        let errorMessage = objectQuery(err, 'response', 'message') || objectQuery(err, 'response') || T.translate(`${PREFIX}.defaultTestErrorMessage`);
          this.setState({
            result: {
              type: CARD_ACTION_TYPES.DANGER,
              message: errorMessage
            }
          });
        });
  }

  renderActionButton() {
    let disabled = this.isButtonDisabled();

    return (
      <ModalFooter>
        <button
          className="btn btn-primary"
          onClick={this.apply}
          disabled={disabled}
        >
          {T.translate(`${PREFIX}.Buttons.Save`)}
        </button>

        <button
          className="btn btn-primary"
          onClick={this.toggleModal}
        >
          {T.translate(`${PREFIX}.Buttons.Cancel`)}
        </button>
      </ModalFooter>
    );
  }

  renderMessage() {
    if (!this.state.result.message) { return null; }
    const resultType = this.state.result.type;
    return (
      <CardActionFeedback
        message={T.translate(`${PREFIX}.MessageLabels.${resultType.toLowerCase()}`)}
        extendedMessage={resultType === CARD_ACTION_TYPES.SUCCESS ? null : this.state.result.message}
        type={resultType}
      />
    );
  }

  render() {
    return (
      <Modal
        isOpen={this.state.isOpen}
        toggle={this.toggleModal}
        size="lg"
        className="add-secured-key-modal cdap-modal"
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

          <div className="add-secured-key-modal-detail">

            <div className="form">

              {/* secured key name */}
              <div className="form-group row">
                <label className={LABEL_COL_CLASS}>
                  {T.translate(`${PREFIX}.name`)}
                  { this.state.inputs['name']['required'] &&
                    <span className="asterisk">*</span>
                  }
                </label>
                <div className={INPUT_COL_CLASS}>
                  <ValidatedInput
                    type="text"
                    label={this.state.inputs['name']['label']}
                    validationError={this.state.inputs['name']['error']}
                    value={this.state.name}
                    onChange={this.handleChange.bind(this, 'name')}
                    placeholder={T.translate(`${PREFIX}.Placeholders.name`)}
                  />
                </div>
              </div>

              {/* secured key description */}
              <div className="form-group row">
                <label className={LABEL_COL_CLASS}>
                  {T.translate(`${PREFIX}.description`)}
                  { this.state.inputs['description']['required'] &&
                    <span className="asterisk">*</span>
                  }
                </label>
                <div className={INPUT_COL_CLASS}>
                  <ValidatedInput
                    type="text"
                    label={this.state.inputs['description']['label']}
                    validationError={this.state.inputs['description']['error']}
                    value={this.state.description}
                    onChange={this.handleChange.bind(this, 'description')}
                    placeholder={T.translate(`${PREFIX}.Placeholders.description`)}
                  />
                </div>
              </div>

              {/* secured key data */}
              <div className="form-group row">
                <label className={LABEL_COL_CLASS}>
                  {T.translate(`${PREFIX}.data`)}
                  { this.state.inputs['data']['required'] &&
                    <span className="asterisk">*</span>
                  }
                </label>
                <div className={INPUT_COL_CLASS}>
                  <ValidatedInput
                    type="text"
                    label={this.state.inputs['data']['label']}
                    validationError={this.state.inputs['data']['error']}
                    value={this.state.data}
                    onChange={this.handleChange.bind(this, 'data')}
                    placeholder={T.translate(`${PREFIX}.Placeholders.data`)}
                  />
                </div>
              </div>

              {this.renderSecuredKeyProperties()}

            </div>
          </div>

        </ModalBody>
        {this.renderActionButton()}
        {this.renderMessage()}
      </Modal>
    );
  }
}

AddSecuredKeyModal.propTypes = {
  isOpen: PropTypes.bool,
  toggle: PropTypes.func
};
