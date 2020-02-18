import React from 'react';
import PropTypes from 'prop-types';
import SecuredKeyGrid from 'components/SecuredKeyGrid';
import { getCurrentNamespace } from 'services/NamespaceStore';
import { MySecureKeyApi } from 'api/securekey';
import {Observable} from 'rxjs/Observable';

require('./SecuredKeyInterface.scss');

export default class SecuredKeyInterface extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    handleClose: PropTypes.func
  }

  state = {
    securedKeys: {},
    securedKeysData: []
  }
  componentDidMount() {
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
  render() {
    return (
      <SecuredKeyGrid data={this.state.securedKeysData}/>
    );
  }
}
