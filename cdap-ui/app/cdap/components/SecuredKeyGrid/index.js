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
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-balham.css';
import isEmpty from 'lodash/isEmpty';
import PropTypes from 'prop-types';
import GridActionRenderer from './GridActionRenderer';
import { SecuredKeysGridCols, COPY_TO_CLIPBOARD, SHOW_KEY_DATA, DELETE_KEY } from './constants';

require('./SecuredKeyGrid.scss');

class SecuredKeyGrid extends React.Component {
  gridApi;
  gridColumnApi;

  constructor(props) {
    super(props);
    this.state = {
      columnDefs: SecuredKeysGridCols,
      frameworkComponents: {
        'gridActionRenderer': GridActionRenderer,
      },
      context: { componentParent: this },
    };
  }

  componentWillReceiveProps(nextProps) {
    if (this.gridApi) {
      if (nextProps.isDataLoading) {
        this.gridApi.showLoadingOverlay();
      } else {
        if (isEmpty(nextProps.data)) {
          this.gridApi.showNoRowsOverlay();
        } else {
          this.gridApi.hideOverlay();
        }
      }
    }
  }

  onGridReady = params => {
    this.gridApi = params.api;
    this.gridColumnApi = params.columnApi;
  }

  componentDidUpdate() {
    if (this.gridApi) {
      this.gridApi.setQuickFilter(this.props.searchText);
    }
  }

  render() {
    setTimeout(() => {
      if (this.gridApi) {
        this.gridApi.sizeColumnsToFit();
      }
    }, 500);
    return (
      <div className="ag-theme-balham grid-container">
        <AgGridReact
          suppressMenuHide={true}
          columnDefs={this.state.columnDefs}
          context={this.state.context}
          frameworkComponents={this.state.frameworkComponents}
          rowData={this.props.data}
          onGridReady={this.onGridReady}
        >
        </AgGridReact>
      </div>
    );
  }

  onAction(item, type) {
    switch (type) {
      case COPY_TO_CLIPBOARD:
        this.onCopyToClipboard(item);
        break;
      case SHOW_KEY_DATA:
        this.onShowKeyData(item);
        break;
      case DELETE_KEY:
        this.onDeleteKey(item);
        break;
    }
  }

  onCopyToClipboard(item) {
    if (this.props.onCopyToClipboard) {
      this.props.onCopyToClipboard(item);
    }
  }

  onShowKeyData(item) {
    if (this.props.onShowKeyData) {
      this.props.onShowKeyData(item);
    }
  }

  onDeleteKey(item) {
    if (this.props.onDeleteKey) {
      this.props.onDeleteKey(item);
    }
  }
}

export default SecuredKeyGrid;
SecuredKeyGrid.propTypes = {
  isDataLoading: PropTypes.any,
  data: PropTypes.array,
  onCopyToClipboard: PropTypes.func,
  onShowKeyData: PropTypes.func,
  onDeleteKey: PropTypes.func,
  searchText: PropTypes.string
};
