/*
 * Copyright © 2016 Cask Data, Inc.
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
import React, { Component, PropTypes } from 'react';
import WizardModal from 'components/WizardModal';
import Wizard from 'components/Wizard';
import PublishPipelineWizardStore from 'services/WizardStores/PublishPipeline/PublishPipelineStore';
import PublishPipelineWizardConfig from 'services/WizardConfigs/PublishPipelineWizardConfig';
import PublishPipelineAction from 'services/WizardStores/PublishPipeline/PublishPipelineActions.js';
import PublishPipelineActionCreator from 'services/WizardStores/PublishPipeline/ActionCreator.js';
import head from 'lodash/head';
import shortid from 'shortid';
import MyUserStoreApi from 'api/userstore';
import {default as NamespaceStore} from 'services/store/store';

import T from 'i18n-react';

export default class PublishPipelineWizard extends Component {
  constructor(props) {
    super(props);
    this.state = {
      showWizard: this.props.isOpen
    };
  }
  componentWillMount() {
    let action = this.props.input.action;
    let filename = head(action.arguments.filter(arg => arg.name === 'config')).value;
    PublishPipelineActionCreator
      .fetchPipelineConfig({
        entityName: this.props.input.package.name,
        entityVersion: this.props.input.package.version,
        filename
      });
  }
  toggleWizard(returnResult) {
    if (this.state.showWizard) {
      this.props.onClose(returnResult);
    }
    this.setState({
      showWizard: !this.state.showWizard
    });
  }
  componentWillReceiveProps({isOpen}) {
    this.setState({
      showWizard: isOpen
    });
  }
  componentWillUnmount() {
    PublishPipelineWizardStore.dispatch({
      type: PublishPipelineAction.onReset
    });
  }
  publishPipeline() {
    let action = this.props.input.action;
    let artifact = head(action.arguments.filter(arg => arg.name === 'artifact')).value;
    let {name, pipelineConfig} = PublishPipelineWizardStore.getState().pipelinemetadata;
    let draftConfig = {
      artifact,
      config: pipelineConfig,
      name: name
    };
    return MyUserStoreApi
      .get()
      .flatMap((res) => {
        let currentNamespace = NamespaceStore.getState().selectedNamespace;
        let draftId = shortid.generate();
        res = res || {};
        res.hydratorDrafts = res.hydratorDrafts || {};
        res.hydratorDrafts[currentNamespace] = res.hydratorDrafts[currentNamespace] || {};
        res.hydratorDrafts[currentNamespace][draftId] = draftConfig;
        return MyUserStoreApi.set({}, res);
      });
  }
  render() {
    let wizardModalTitle = (this.props.input.package.label ? this.props.input.package.label + " | " : '') + T.translate('features.Wizard.PublishPipeline.headerlabel') ;
    return (
      <div>
        {
          this.state.showWizard ?
            // eww..
            <WizardModal
              title={wizardModalTitle}
              isOpen={this.state.showWizard}
              toggle={this.toggleWizard.bind(this, false)}
              className="create-stream-wizard"
            >
              <Wizard
                wizardConfig={PublishPipelineWizardConfig}
                onSubmit={this.publishPipeline.bind(this)}
                onClose={this.toggleWizard.bind(this)}
                store={PublishPipelineWizardStore}/>
            </WizardModal>
          :
            null
        }
      </div>
    );
  }
}
PublishPipelineWizard.propTypes = {
  isOpen: PropTypes.bool,
  input: PropTypes.any,
  onClose: PropTypes.func
};
