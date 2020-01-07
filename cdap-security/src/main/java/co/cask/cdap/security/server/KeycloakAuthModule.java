package co.cask.cdap.security.server;

import org.eclipse.jetty.security.jaspi.modules.BaseAuthModule;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class KeycloakAuthModule extends BaseAuthModule{
    private static final Logger LOG = Log.getLogger(org.eclipse.jetty.security.jaspi.modules.BasicAuthModule.class);
    private String realmName;
    private static final String REALM_KEY = "org.eclipse.jetty.security.jaspi.modules.RealmName";

    public KeycloakAuthModule() {
    }

    public KeycloakAuthModule(CallbackHandler callbackHandler, String realmName) {
        super(callbackHandler);
        this.realmName = realmName;
    }

    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        super.initialize(requestPolicy, responsePolicy, handler, options);
        this.realmName = (String)options.get("org.eclipse.jetty.security.jaspi.modules.RealmName");
    }

    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse)messageInfo.getResponseMessage();
        String credentials = request.getHeader("Authorization");

        try {
            if (credentials != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Credentials: " + credentials, new Object[0]);
                }

                if (this.login(clientSubject, credentials, "BASIC", messageInfo)) {
                    return AuthStatus.SUCCESS;
                }
            }

            if (request.getHeader("keycloakToken")!=null){
                return AuthStatus.SUCCESS;
            }

            if (!this.isMandatory(messageInfo)) {
                return AuthStatus.SUCCESS;
            } else {
                response.setHeader("WWW-Authenticate", "basic realm=\"" + this.realmName + '"');
                response.sendError(401);
                return AuthStatus.SEND_CONTINUE;
            }
        } catch (IOException var8) {
            throw new AuthException(var8.getMessage());
        } catch (UnsupportedCallbackException var9) {
            throw new AuthException(var9.getMessage());
        }
    }
}
