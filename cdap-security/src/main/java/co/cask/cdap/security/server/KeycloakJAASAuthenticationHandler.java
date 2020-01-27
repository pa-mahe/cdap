package co.cask.cdap.security.server;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.jaspi.ServletCallbackHandler;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Map;

public class KeycloakJAASAuthenticationHandler extends JASPIAuthenticationHandler {

    @Override
    public void init(Map<String, String> handlerProp) throws Exception {
        super.init(handlerProp);
    }

    @Override
    protected Authenticator getHandlerAuthenticator() {
        ServletCallbackHandler callbackHandler = new ServletCallbackHandler(getHandlerLoginService());
        ServerAuthModule authModule = new KeycloakAuthModule(callbackHandler, "JAASRealm",handlerProps);
        return getHandlerAuthenticatorProvider(authModule, callbackHandler);
    }
    @Override
    protected boolean isEmbeddTokenRequired(){
        return true;
    }
}
