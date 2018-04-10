package edu.pugetsound.util;

import javax.net.ssl.SSLContext;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

/**
 * To compile:
 * (Change / to backslash on Windows)
 * Place this file in a directory tree edu/pugetsound/util below your current
 * working location. Source and target parameters below are necessary to work 
 * with the Java shipped with PeopleTools 8.55.
 *
 * javac -source 1.7 -target 1.7 -cp httpclient-4.5.5.jar;httpcore-4.4.9.jar edu/pugetsound/util/JsonPost.java
 * jar cvf up_jsonpost.jar edu/pugetsound/util/*.class
 *
 * Place the resulting up_jsonpost.jar along with the httpclient and httpcore jars 
 * in the class folder under your PeopleTools installation. You may need to bounce 
 * Process Scheduler to get Application Engine programs to see the jar.
 */
 
public class JsonPost
{
    /* Object properties */
    private String serviceUrl;
    private String contentType;
    private String jsonMessage;
    private String authHost;
    private String authPort;
    private String authRealm;
    private String authUsername;
    private String authPassword;
    private String serviceResponse;
    private int statusCode;
    private String statusReason;
    
    /* Storing the last exception and whether an exception was thrown.
       This is to work around PeopleSoft's difficulties with Java exceptions.
    */
    private boolean exceptionWasThrown;
    private Exception thrownException;
    
    public JsonPost()
    {
        /* When we start, we have not thrown an exception yet. */
        this.exceptionWasThrown = false;
    }
    
    /* Input properties to JSON post */
    
    public void setServiceUrl(final String serviceUrl)
    {
        this.serviceUrl = serviceUrl;
    }
    
    public void setContentType(final String contentType)
    {
        this.contentType = contentType;
    }
    
    public void setJsonMessage(final String jsonMessage)
    {
        this.jsonMessage = jsonMessage;
    }

    public void setAuthHost(final String authHost)
    {
        this.authHost = authHost;
    }
    
    public void setAuthPort(final String authPort)
    {
        this.authPort = authPort;
    }
    
    public void setAuthRealm(final String authRealm)
    {
        this.authRealm = authRealm;
    }
    
    public void setAuthUsername(final String authUsername)
    {
        this.authUsername = authUsername;
    }
    
    public void setAuthPassword(final String authPassword)
    {
        this.authPassword = authPassword;
    }
    
    /* Here the real work happens */
    
    public void execute()
    {
        this.exceptionWasThrown = false;
        try
        {
            /* Create an HttpClient that will use TLS v1.2 */
            SSLContext sslContext = SSLContext.getDefault();
            String[] protocols = new String[] {"TLSv1.2"};
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
              sslContext, 
              protocols, 
              null, 
              SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            );
            CloseableHttpClient client = HttpClientBuilder
              .create()
              .setSSLSocketFactory(socketFactory)
              .build();
            
            
            /* Create the method that will POST to the serviceUrl */
            HttpPost post = new HttpPost(serviceUrl);
            
            /* Create the JSON message and attach it to the POST method */
            StringEntity inputEntity;
            if (contentType == null)
            {
                inputEntity = new StringEntity(jsonMessage);
            }
            else
            {
                inputEntity = new StringEntity(jsonMessage, ContentType.create(contentType));
            }
            post.setEntity(inputEntity);
            
            /* POST the message */
            /* If I'm authenticating, set up a context with a credentials provider to use */
            CloseableHttpResponse response;
            if (this.authUsername != null && this.authPassword != null)
            {
                /* First create the credentials provider */
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                String authHost = (this.authHost == null) ? AuthScope.ANY_HOST : this.authHost;
                int authPort = (this.authPort == null) ? AuthScope.ANY_PORT : Integer.decode(this.authPort);
                String authRealm = (this.authRealm == null) ? AuthScope.ANY_REALM : this.authRealm;
                credsProvider.setCredentials(new AuthScope(authHost, authPort, authRealm), new UsernamePasswordCredentials(authUsername, authPassword));
                /* Then create the context and attach the credentials provider to it */
                HttpClientContext context = HttpClientContext.create();
                context.setCredentialsProvider(credsProvider);
                /* Now execute the request using the context */
                response = client.execute(post, context);
            }
            else
            {
                response = client.execute(post);
            }
            
            /* Get the output from the response and set the status code */
            HttpEntity outputEntity = response.getEntity();
            this.serviceResponse = EntityUtils.toString(outputEntity);
            this.statusCode = response.getStatusLine().getStatusCode();
            this.statusReason = response.getStatusLine().getReasonPhrase();
        }
        catch (Exception e)
        {
            /* Set the flag that an exception was thrown and stash the exception */
            this.exceptionWasThrown = true;
            this.thrownException = e;
        }
    }
    
    /* Output properties from JSON post */
    
    public String getServiceResponse()
    {
        return this.serviceResponse;
    }
    
    public String getStatusCode()
    {
        return Integer.toString(this.statusCode);
    }
    
    public String getStatusReason()
    {
        return this.statusReason;
    }
    
    /* Exposing last thrown exception info to PeopleSoft */
    
    public boolean getExceptionWasThrown()
    {
        return this.exceptionWasThrown;
    }
    
    public Exception getThrownException()
    {
        return this.thrownException;
    }
    
    public String getExceptionInfo()
    {
        /* We're dumping out everything we know about the thrown exception,
           including nested exceptions (the "causes").
        */
        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();
        Throwable e = this.thrownException;
                
        do
        {
            sb.append("Exception class: ");
            sb.append(e.getClass());
            sb.append(nl);
            sb.append("Exception message: ");
            sb.append(e.getMessage());
            sb.append(nl);
            sb.append("Exception stack trace: ");
            sb.append(nl);
            for (StackTraceElement stackElement : e.getStackTrace())
            {
                sb.append(" ");
                sb.append(stackElement.toString());
                sb.append(nl);
            }
            if (e.getCause() != null)
            {
                sb.append("---");
                sb.append(nl);
            }
            e = e.getCause();
        } while (e != null);
        
        return sb.toString();
    }
}