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
import { getEpochDateString } from "./utils";

// Actions
export const COPY_TO_CLIPBOARD = "COPY_TO_CLIPBOARD";
export const DELETE_KEY = "DELETE_KEY";
export const SHOW_KEY_DATA = "SHOW_KEY_DATA";
export const ADD_NEW_KEY = "ADD_NEW_KEY";

export const SecuredKeysGridCols = [
  {
    headerName: "Name",
    field: "name",
    tooltipField: "name",
    maxWidth: 300,
    sortable: true,
    getQuickFilterText(params) {
      return params.value;
    },
  },
  {
    headerName: "Description",
    field: "description",
    tooltipField: "description",
    flex: 1,
  },
  {
    headerName: "Created Time",
    field: "createdEpochMs",
    tooltipField: "createdEpochMs",
    sortable: true,
    valueFormatter(params) {
      return getEpochDateString(params);
    },
    suppressMenu: true,
    maxWidth: 250,
  },
  {
    headerName: "",
    field: "copy_to_clipboard",
    maxWidth: 50,
    cellRenderer: "gridActionRenderer",
    cellRendererParams: { action: COPY_TO_CLIPBOARD },
    suppressMenu: true,
    cellClass: 'secured-key-action',
  },
  {
    headerName: "",
    field: "show",
    maxWidth: 50,
    cellRenderer: "gridActionRenderer",
    cellRendererParams: { action: SHOW_KEY_DATA },
    suppressMenu: true,
    cellClass: 'secured-key-action',
  },
  {
    headerName: "",
    field: "delete",
    maxWidth: 50,
    cellRenderer: "gridActionRenderer",
    cellRendererParams: { action: DELETE_KEY },
    suppressMenu: true,
    cellClass: 'secured-key-action',
  },
];
