package co.cask.cdap.security.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.security.jaspi.modules.BasicAuthModule;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class KeycloakAuthModule extends BasicAuthModule {
    private static final Logger LOG = Log.getLogger(org.eclipse.jetty.security.jaspi.modules.BasicAuthModule.class);
    private String realmName;
    private static KeycloakDeployment deployment;
    private static Map<String, String> handlerProps;

    public KeycloakAuthModule() {
    }

    public KeycloakAuthModule(CallbackHandler callbackHandler, String realmName, Map<String, String> handlerProps) {
        super(callbackHandler, realmName);
        this.realmName = realmName;
        this.handlerProps = handlerProps;
        deployment = createKeycloakDeployment();
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
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

            if (request.getHeader("keycloakToken") != null) {
                String keycloakTokenString = request.getHeader("keycloakToken");
                AccessToken keycloakAccessToken = verifyKeycloakToken(keycloakTokenString);
<<<<<<< HEAD
                request.setAttribute("keycloakAccessToken",keycloakAccessToken);

=======
                request.setAttribute("keycloakAccessToken", keycloakAccessToken);
>>>>>>> 0291a7c39c... exception Handling fix
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
        } catch (VerificationException ex) {
            response.setStatus(401);
            //throw new AuthException("Keycloak Token is invalid");
        }
<<<<<<< HEAD
=======
        return AuthStatus.SEND_CONTINUE;
>>>>>>> 0951838a18... minor review changes
    }

    private AccessToken verifyKeycloakToken(String keycloakTokenString) throws VerificationException {
        AccessToken keycloakAccessToken;
        keycloakAccessToken = AdapterTokenVerifier.verifyToken(keycloakTokenString, deployment);
        return keycloakAccessToken;
    }

    private static KeycloakDeployment createKeycloakDeployment() {
        if (deployment != null)
            return deployment;

        try {
            createKeycloakConfigurationFile();
            String filepath = handlerProps.get("keycloak-config-file");
            InputStream inputStream = new FileInputStream(new File(filepath));
            deployment = KeycloakDeploymentBuilder.build(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Keycloak config file not found on the path");
        } catch (Exception ex) {
            throw new RuntimeException("Error Occured while creating keycloak deployment");
        }
        return deployment;
    }

    private static void createKeycloakConfigurationFile() {
        try {
            String clientId = handlerProps.get("client_id");
            String clientSecret = handlerProps.get("client_secret");
            String realm = handlerProps.get("realm");
            String authServerUrl = handlerProps.get("authserverurl");
            Map<String, Object> clientCredentials = new HashMap();
            clientCredentials.put("secret", clientSecret);
            org.keycloak.authorization.client.Configuration keycloakConf = new org.keycloak.authorization.client.Configuration(authServerUrl, realm, clientId, clientCredentials, null);
            ObjectMapper objectMapper = new ObjectMapper();

            String filePath = handlerProps.get("keycloak-config-file");
            objectMapper.writeValue(new File(filePath), keycloakConf);
        } catch (Exception ex) {
            throw new RuntimeException("Error while generating keycloak configuration file");
        }
    }
}
