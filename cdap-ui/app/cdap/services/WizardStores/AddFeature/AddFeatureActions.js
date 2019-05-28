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

const AddFeatureActions = {
  saveFeature: 'SAVE-FEATURE',
  onReset: 'RESET-STORE',
  updateOperationType: 'UPDATE-OPERATION-TYPE',
  updateFeatureName: 'UPDATE-FEATURE-NAME',
  setAvailableSchemas: 'SET-AVAILABLE-SCHEMAS',
  setAvailableProperties: 'SET-AVAILABLE-PROPERTIES',
  setAvailableConfigurations: 'SET-AVAILABLE-CONFIGURATIONS',
  setAvailableSinks: 'SET-AVAILABLE-SINKS',
  setSelectedSchemas: 'SET-SELECTED-SCHEMAS',
  updateSelectedSchema: 'UPDATE-SELECTED-SCHEMA',
  deleteSelectedSchema: 'DELETE-SELECTED-SCHEMA',
  updatePropertyMap: 'UPDATE-PROPERTY-MAP',
  updateConfigurationList: 'UPDATE-CONFIGURATION-LIST',
  setDetectedProperties: 'SET-DETECTED-PROPERTIES',
  setSinkConfigurations: 'SET-SINK-CONFIGURATIONS',

};

export default AddFeatureActions;