import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import PropTypes from 'prop-types';
import * as util from './utils';
import cookie from 'react-cookie';

class Secured extends Component {

  keycloakConfig = {
    'realm': "dev",
    'url': "http://192.168.154.194:8180/auth",
    'clientId': 'backend-client',
    'credentials': {
      secret: 'd0c0dd65-28fe-4bae-a35e-010651bce3f2'
    }
  };

  constructor(props) {
    super(props);
    this.state = { keycloak: null, authenticated: false };
  }

  componentDidMount() {
    const keycloak = Keycloak(this.keycloakConfig);
    keycloak.init(
      {
        onLoad: 'login-required',
        checkLoginIframe: false,
        promiseType: 'native'
      }).then(authenticated => {
        console.log("KeyCloak -> ", authenticated);
        this.setState({ keycloak: keycloak, authenticated: authenticated });
        cookie.save('Keycloak_Refresh_Token', keycloak.refreshToken, { path: '/'});
        cookie.save('Keycloak_Token', keycloak.token, { path: '/'});
        this.setKeyCloackAuthentication(authenticated);
      });
  }

  setKeyCloackAuthentication(authenticated) {
    if (this.props.setKeyCloackAuthentication) {
      this.props.setKeyCloackAuthentication(authenticated);
    }
  }

  render() {
    if (this.state.keycloak) {
      if (this.state.authenticated) {
        return (<div></div>);
      } else {
        return (<div>Unable to authenticate!</div>);
      }
    }
    return (
      <div>Initializing Keycloak...</div>
    );
  }
}
Secured.propTypes = {
  setKeyCloackAuthentication: PropTypes.func
};
export default Secured;
