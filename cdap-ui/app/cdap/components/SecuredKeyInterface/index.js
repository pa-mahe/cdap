import React from 'react';
import PropTypes from 'prop-types';
import SecuredKeyGrid from 'components/SecuredKeyGrid';
import { getCurrentNamespace } from 'services/NamespaceStore';
import { MySecureKeyApi } from 'api/securekey';

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
  componentWillMount() {
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
        const promises = [];
        keys.forEach(key => {
          promises.push(new Promise(function (resolve) {
            MySecureKeyApi.metadata({ namespace, id: key });
          }));
        });

        Promise.all(promises).then(function (values) {
          console.log(values);
        });
      });
  }
  render() {
    return (
      <SecuredKeyGrid />
    );
  }
}
