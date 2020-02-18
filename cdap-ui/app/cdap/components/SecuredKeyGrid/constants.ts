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
    width: 400,
  },
  {
    headerName: "Description",
    field: "description",
    tooltipField: "description",
    width: 300,
  },
  {
    headerName: "Created Time",
    field: "createdEpochMs",
    tooltipField: "createdEpochMs",
    valueFormatter: function(params) {
      return getEpochDateString(params);
    },
    suppressMenu: true,
    width: 300,
  },
  {
    headerName: "",
    field: "copy_to_clipboard",
    width: 40,
    cellRenderer: "actionRenderer",
    cellRendererParams: { action: COPY_TO_CLIPBOARD },
    suppressMenu: true,
  },
  {
    headerName: "",
    field: "delete",
    width: 40,
    cellRenderer: "actionRenderer",
    cellRendererParams: { action: DELETE_KEY },
    suppressMenu: true,
  },
  {
    headerName: "",
    field: "show",
    width: 40,
    cellRenderer: "actionRenderer",
    cellRendererParams: { action: SHOW_KEY_DATA },
    suppressMenu: true,
  },
];
