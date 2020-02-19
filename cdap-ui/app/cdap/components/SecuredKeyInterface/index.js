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
import T from 'i18n-react';

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
  };

  constructor(props) {
    super(props);

    this.toggelAddSecuredKeyModal = this.toggelAddSecuredKeyModal.bind(this);
  }

  toggelAddSecuredKeyModal() {
    this.setState({
      addSecuredKeyModalOpen: !this.state.addSecuredKeyModalOpen
    });
  }

  componentDidMount() {
    this.getKeys();
  }

  getKeys() {
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
        keys.forEach(key => {
          observableArr.push(MySecureKeyApi.metadata({ namespace, id: key }));
        });
        Observable.forkJoin(
          observableArr
        ).subscribe((res) => {
          this.setState({
            securedKeysData: res
          });
        }, (err) => {
          console.log(err);
        });
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

  onDeleteKey(item) {
    if (item && item.name) {
      console.log(item.name);
    }
  }

  render() {
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
            searchText={this.props.searchText}
          />
        </div>
      </div>
    );
  }
}

SecuredKeyInterface.propTypes = {
  searchText: PropTypes.string,
  getKeys: PropTypes.func,
};
