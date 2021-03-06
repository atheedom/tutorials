package org.baeldung.httpclient;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class SandboxTest {

    @Test
    public final void whenInterestingDigestAuthScenario_then200OK() throws AuthenticationException, ClientProtocolException, IOException, MalformedChallengeException {
        final HttpHost targetHost = new HttpHost("httpbin.org", 80, "http");

        // set up the credentials to run agains the server
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), new UsernamePasswordCredentials("user", "passwd"));

        // We need a first run to get a 401 to seed the digest auth

        // Make a client using those creds
        final CloseableHttpClient client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        // And make a call to the URL we are after
        final HttpGet httpget = new HttpGet("http://httpbin.org/digest-auth/auth/user/passwd");

        // Create a context to use
        final HttpClientContext context = HttpClientContext.create();

        // Get a response from the sever (expect a 401!)
        final HttpResponse authResponse = client.execute(targetHost, httpget, context);

        // Pull out the auth header that came back from the server
        final Header challenge = authResponse.getHeaders("WWW-Authenticate")[0];

        // Lets use a digest scheme to solve it
        final DigestScheme digest = new DigestScheme();
        digest.processChallenge(challenge);

        // Make a header with the solution based upon user/password and what the digest got out of the initial 401 reponse
        final Header solution = digest.authenticate(new UsernamePasswordCredentials("user", "passwd"), httpget, context);

        // Need an auth cache to use the new digest we made
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, digest);
        
        // Add the authCache and thus solved digest to the context
        context.setAuthCache(authCache);

        // Pimp up our http get with the solved header made by the digest
        httpget.addHeader(solution);

        // use it!
        System.out.println("Executing request " + httpget.getRequestLine() + " to target " + targetHost);

        for (int i = 0; i < 3; i++) {
            final CloseableHttpResponse responseGood = client.execute(targetHost, httpget, context);

            try {
                System.out.println("----------------------------------------");
                System.out.println(responseGood.getStatusLine());
                System.out.println(EntityUtils.toString(responseGood.getEntity()));
            } finally {
                responseGood.close();
            }
        }

    }
}
