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
import DraftTableRow from 'components/PipelineList/DraftPipelineView/DraftTable/DraftTableRow';
import T from 'i18n-react';

require('./DraftTable.scss');

const PREFIX = 'features.PipelineList';

export default class DraftTable extends Component {
  static propTypes = {
    drafts: PropTypes.array
  };

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div className="draft-table">
        <div className="table-header">
          <div className="table-column name">
            {T.translate(`${PREFIX}.pipelineName`)}
          </div>
          <div className="table-column type">
            {T.translate(`${PREFIX}.type`)}
          </div>
          <div className="table-column creation-time">
            {T.translate(`${PREFIX}.creationTime`)}
          </div>
          <div className="table-column action"></div>
        </div>

        <div className="table-body">
          {
            this.props.drafts.map((draft) => {
              return (
                <DraftTableRow
                  draft={draft}
                  key={draft.__ui__.draftId}
                />
              );
            })
          }
        </div>
      </div>
    );
  }
}