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
import { COPY_TO_CLIPBOARD, DELETE_KEY, SHOW_KEY_DATA } from '../constants';
import T from 'i18n-react';
const PREFIX = 'features.SecureKeyStorage.Actions';


class ActionRenderer extends React.Component {
  constructor(props) {
    super(props);
  }


  invokeParentMethod(item, type) {
    if (this.props.context && this.props.context.componentParent && this.props.context.componentParent.onAction) {
      this.props.context.componentParent.onAction(item, type);
    }
  }


  render() {

    let type = this.props.colDef.cellRendererParams.action;
    let hoverTitle = '';
    let actionClass =  "grid-item-action fa";
    if (type == COPY_TO_CLIPBOARD) {
      actionClass += " fa-clipboard";
      hoverTitle =  T.translate(`${PREFIX}.copyToClipboard`);
    } else if (type == SHOW_KEY_DATA) {
      actionClass += " fa-eye";
      hoverTitle =  T.translate(`${PREFIX}.showKey`);
    } else if (type == DELETE_KEY) {
      actionClass += " fa-trash text-danger";
      hoverTitle =  T.translate(`${PREFIX}.deleteKey`);
    }

    return (<span className={actionClass}
      onClick={this.invokeParentMethod.bind(this, this.props.data, type)} title = {hoverTitle}>
    </span>);

  }
}
export default ActionRenderer;
ActionRenderer.propTypes = {
  context: PropTypes.object,
  data: PropTypes.any,
  colDef:PropTypes.object
};
