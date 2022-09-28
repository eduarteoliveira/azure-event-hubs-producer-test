package org.cerc.billing.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.cerc.billing.authenticate.eventhubs.OAuthBearerTokenImp;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

public class CustomAuthenticateGCPAzureFederatedCredentialsCallbackHandler
		implements
			AuthenticateCallbackHandler {

	private static final String GCP_SERVICE_ACCOUNT_DEFAULT = "default";
	private final static String GCP_URI_GET_TOKEN = "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/%s/identity?audience=api://AzureADTokenExchange";

	OAuthBearerToken getOAuthBearerToken() throws Exception {
		String token = tokenGCP(GCP_SERVICE_ACCOUNT_DEFAULT);
		System.out.println("Gerou o token ----> ");
		System.out.println(token);
		System.out.println("----------------------------------------");
		JWT jwt = JWTParser.parse(token);
		JWTClaimsSet claims = jwt.getJWTClaimsSet();

		return new OAuthBearerTokenImp(token, claims.getExpirationTime());
	}

	public void handle(Callback[] callbacks)
			throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof OAuthBearerTokenCallback) {
				try {
					OAuthBearerToken token = getOAuthBearerToken();
					OAuthBearerTokenCallback oauthCallback = (OAuthBearerTokenCallback) callback;
					oauthCallback.token(token);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				throw new UnsupportedCallbackException(callback);
			}
		}
	}

	private String tokenGCP(String serviceAccount) throws Exception {
		BufferedReader br = null;
		String token = null;

		URL url = new URL(String.format(GCP_URI_GET_TOKEN, serviceAccount));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Metadata-Flavor", "Google");
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		Integer responseCode = connection.getResponseCode();

		if (responseCode < 200 || responseCode > 299) {
			Scanner scanner = new Scanner(connection.getErrorStream());
			String erro = scanner.useDelimiter("\\Z").next();
			scanner.close();
			throw new Exception(erro);
		} else {
			br = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			String value;
			while ((value = br.readLine()) != null) {
				token = value;
			}
		}

		connection.disconnect();

		return token;
	}

	@Override
	public void close() {
	}

	@Override
	public void configure(Map<String, ?> configs, String mechanism,
			List<AppConfigurationEntry> jaasConfigEntries) {
	}
}
