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

import React from 'react';
import PropTypes from 'prop-types';
import SecuredKeyGrid from 'components/SecuredKeyGrid';
import { getCurrentNamespace } from 'services/NamespaceStore';
import { MySecureKeyApi } from 'api/securekey';
import { Observable } from 'rxjs/Observable';
import { copyToClipboard } from 'components/SecuredKeyGrid/utils';
import AddSecuredKeyModal from 'components/SecuredKeyInterface/AddSecuredKeyModal';
import ConfirmationModal from 'components/ConfirmationModal';
import LoadingSVG from 'components/LoadingSVG';
import {objectQuery, isNilOrEmpty} from 'services/helpers';

import T from 'i18n-react';
import { KEY_ADDITION_SUCCESS } from './constants';
import SecuredKeyDataModal from './SecuredKeyDataModal';

require('./SecuredKeyInterface.scss');
const PREFIX = 'features.SecuredKeyModal';

export default class SecuredKeyInterface extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    handleClose: PropTypes.func
  }

  state = {
    securedKeys: {},
    securedKeysData: [],
    addSecuredKeyModalOpen: false,
    error: null,
    searchText: '',
    searchFocus: true,
    deleteModalOpen: false,
    deleteErrMsg: '',
    extendedDeleteErrMsg: '',
    selectedItem: null,
    loading: false,
    currentSecuredKeyData: '',
    currentSecuredKeyName: '',
    openSecuredKeyDataModal: false,
  };

  constructor(props) {
    super(props);

    this.toggelAddSecuredKeyModal = this.toggelAddSecuredKeyModal.bind(this);
  }

  toggelAddSecuredKeyModal(status) {
    this.setState({
      addSecuredKeyModalOpen: !this.state.addSecuredKeyModalOpen
    });
    if (!isNilOrEmpty(status) && status == KEY_ADDITION_SUCCESS) {
        this.getKeys();
    }
  }

  toggleDeleteConfirmationModal() {
    this.setState({
      deleteModalOpen: !this.state.deleteModalOpen
    });
  }

  toggleSecuredKeyDataModal() {
    this.setState({
        openSecuredKeyDataModal: !this.state.openSecuredKeyDataModal,
        currentSecuredKeyData: this.state.openSecuredKeyDataModal?'':this.state.currentSecuredKeyData,
    });
  }

  componentDidMount() {
    this.getKeys();
  }

  getKeys() {
    this.setState({ loading: true });
    const namespace = getCurrentNamespace();
    const params = {
      namespace
    };

    MySecureKeyApi.list(params)
      .subscribe((res) => {
        const keys = Object.keys(res);

        this.setState({
          securedKeys: keys
        });
        const observableArr = [];
        if (isNilOrEmpty(keys)) {
            this.setState({
                loading: false
              });
        } else {
            keys.forEach(key => {
              observableArr.push(MySecureKeyApi.metadata({ namespace, id: key }));
            });
            Observable.forkJoin(
              observableArr
            ).subscribe((res) => {
              this.setState({
                securedKeysData: res,
                loading: false
              });
            }, (err) => {
              this.setState({ loading: false });
              console.log(err);
            });
        }
      });
  }

  handleSearch = (e) => {
    this.setState({searchText: e.target.value});
  }

  onCopyToClipboard(item) {
    if (item && item.name) {
      copyToClipboard(item.name);
      this.props.handleClose();
    }
  }

  fetchSecuredKeyData(key) {
    if (!isNilOrEmpty(key)) {
        MySecureKeyApi.retrieve({
            namespace: getCurrentNamespace(),
            name: key
        }).subscribe((res) => {
            this.setState({
                openSecuredKeyDataModal: true,
                currentSecuredKeyName: key,
                currentSecuredKeyData: res,
            });
        }, (err) => {
            this.setState({
                openSecuredKeyDataModal: true,
                currentSecuredKeyName: key,
                currentSecuredKeyData: err.response,
            });
        });
    }
  }

  onShowKeyData(item) {
    if (!isNilOrEmpty(item)) {
        this.fetchSecuredKeyData(item.name);
    }
  }

  onDeleteKey(item) {
    if (item && item.name) {
      this.setState({ selectedItem: item.name });
      this.toggleDeleteConfirmationModal();
    }
  }

  deleteConfirm() {
    const namespace = getCurrentNamespace();

    this.setState({ loading: true });

    MySecureKeyApi.delete({namespace, id:this.state.selectedItem})
      .subscribe(() => {
        this.setState({ loading: false, deleteModalOpen: false });
        this.getKeys();
      }, (err) => {
        this.setState({ loading: false});
        let errorMessage = objectQuery(err, 'response', 'message') || objectQuery(err, 'response') || T.translate(`${PREFIX}.defaultDeleteErrorMessage`);
        this.setState({
          extendedDeleteErrMsg: errorMessage,
          deleteErrMsg: T.translate(`${PREFIX}.deleteError`)
        });
      });
  }

  renderAddSecuredKeyModal() {
    if (!this.state.addSecuredKeyModalOpen) {
      return null;
    }

    return (
      <AddSecuredKeyModal
        isOpen={this.state.addSecuredKeyModalOpen}
        toggle={this.toggelAddSecuredKeyModal}
      />
    );
  }


  renderDeleteConfirmationModal() {
    if (!this.state.deleteModalOpen) {
      return null;
    }

    const confirmationText = T.translate(`${PREFIX}.confirmationText`, {id: this.state.selectedItem});

    return (
      <ConfirmationModal
        headerTitle={T.translate(`${PREFIX}.deleteConfirmationHeader`)}
        toggleModal={this.toggleDeleteConfirmationModal.bind(this, null)}
        confirmationText={confirmationText}
        confirmButtonText={T.translate('commons.delete')}
        confirmFn={this.deleteConfirm.bind(this)}
        cancelFn={this.toggleDeleteConfirmationModal.bind(this, null)}
        isOpen={this.state.deleteModalOpen}
        errorMessage={this.state.deleteErrMsg}
        extendedMessage={this.state.extendedDeleteErrMsg}
      />
    );
  }

  renderSecuredKeyDataModal() {
    return (
      <SecuredKeyDataModal
        handleClose={this.toggleSecuredKeyDataModal.bind(this)}
        title = {this.state.currentSecuredKeyName}
        showModal={this.state.openSecuredKeyDataModal}
        message={this.state.currentSecuredKeyData}
      />
    );
  }

  render() {
    if (this.state.loading) {
      return (
        <div className="text-xs-center">
          <LoadingSVG />
        </div>
      );
    }
    return (
      <div className="secured-key-interface">
        <div className="action-container">
          <div className="search-container">
            <input
              id="secured-key-search-text"
              type="text"
              className="form-control"
              placeholder={T.translate(`${PREFIX}.searchPlaceholder`)}
              value={this.state.searchText}
              onChange={this.handleSearch}
              autoFocus={this.state.searchFocus}
            />
          </div>
          <button className="btn btn-primary"
            onClick={this.toggelAddSecuredKeyModal.bind(this)}>
            {T.translate(`${PREFIX}.addSecuredKeyButtonLabel`)}
          </button>
        </div>
        <div className="secured-key-grid">
          <SecuredKeyGrid
            data={this.state.securedKeysData}
            onCopyToClipboard={this.onCopyToClipboard.bind(this)}
            onShowKeyData={this.onShowKeyData.bind(this)}
            searchText={this.state.searchText}
            onDeleteKey={this.onDeleteKey.bind(this)}
          />
        </div>
        {this.renderAddSecuredKeyModal()}
        {this.renderDeleteConfirmationModal()}
        {this.state.openSecuredKeyDataModal && this.renderSecuredKeyDataModal()}
      </div>
    );
  }
}

SecuredKeyInterface.propTypes = {
  searchText: PropTypes.string,
};
