import React, { Component } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-balham.css';
import './GridContainer.scss'

class GridContainer extends Component {
    constructor(props) {
        super(props);

        this.state = {
            columnDefs: [
                {headerName: "Make", field: "make", width: 250, checkboxSelection: true},
                {headerName: "Model", field: "model"},
                {headerName: "Price", field: "price"}

            ],
            rowData: [
                {make: "Toyota", model: "Celica", price: 35000},
                {make: "Ford", model: "Mondeo", price: 32000},
                {make: "Porsche", model: "Boxter", price: 72000}
            ]
        }
    }

    refreshGridColumns = (data) => {


    }

    refreshGridData = (data) => {

    }

    render() {
        return (
                <div
                  className="ag-theme-balham grid-container"    >
                    <AgGridReact
                        columnDefs={this.state.columnDefs}
                        rowSelection="multiple"
                        rowData={this.state.rowData}>
                    </AgGridReact>
                </div>
            );
    }
}

export default GridContainer;