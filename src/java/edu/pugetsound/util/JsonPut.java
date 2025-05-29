package edu.pugetsound.util;

import javax.net.ssl.SSLContext;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

/**
 * To compile (in 2025, recompiled on Windows):
 * 
 * Unpack the jar in your Downloads directory
 *
 * (Change forward slashes in Windows paths to backslashes)
 * C:/Users/srenker/Downloads>C:/PT8.60.13_Client_ORA/jre/bin/javac -cp C:/PT8.60.13_Client_ORA/class/httpclient.jar;C:/PT8.60.13_Client_ORA/class/httpcore.jar edu/pugetsound/util/JsonPut.java
 * C:/Users/srenker/Downloads>C:/PT8.60.13_Client_ORA/jre/bin/jar cvf up_jsonpost.jar edu/pugetsound/util/*.class
 *
 * Place the resulting up_jsonpost.jar in the class folder under your PeopleSoft application home installation.
 * Bounce Process Scheduler to get Application Engine programs to see the jar.
 */
 
public class JsonPut
{
    /* Object properties */
    private String serviceUrl;
    private String charset;
    private String contentType;
    private String jsonMessage;
    private String authHost;
    private String authPort;
    private String authRealm;
    private String authWorkstation;
    private String authDomain;
    private String authUsername;
    private String authPassword;
    private String serviceResponse;
    private int statusCode;
    private String statusReason;
    private HeaderGroup requestHeaderGroup = new HeaderGroup();
    
    /* Storing the last exception and whether an exception was thrown.
       This is to work around PeopleSoft's difficulties with Java exceptions.
    */
    private boolean exceptionWasThrown;
    private Exception thrownException;
    
    public JsonPut()
    {
        /* When we start, we have not thrown an exception yet. */
        this.exceptionWasThrown = false;
    }
    
    /* Input properties to JSON put */
    
    public void setServiceUrl(final String serviceUrl)
    {
        this.serviceUrl = serviceUrl;
    }
    
    public void setCharset(final String charset)
    {
        this.charset = charset;
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
    
    public void setAuthWorkstation(final String authWorkstation)
    {
        this.authWorkstation = authWorkstation;
    }
    
    public void setAuthDomain(final String authDomain)
    {
        this.authDomain = authDomain;
    }
    
    public void setAuthUsername(final String authUsername)
    {
        this.authUsername = authUsername;
    }
    
    public void setAuthPassword(final String authPassword)
    {
        this.authPassword = authPassword;
    }
    
    /* Passing through methods to manipulate request headers */
    
    public void addRequestHeader(final String name, final String value)
    {
        this.requestHeaderGroup.addHeader(new BasicHeader(name, value));
    }
    
    public void setRequestHeader(final String name, final String value)
    {
        this.requestHeaderGroup.updateHeader(new BasicHeader(name, value));
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
            
            
            /* Create the method that will PUT to the serviceUrl */
            HttpPut put = new HttpPut(serviceUrl);
            
            /* Attach our accumulated headers to the PUT method */
            put.setHeaders(this.requestHeaderGroup.getAllHeaders());
            
            /* Create the JSON message and attach it to the PUT method */
            StringEntity inputEntity;
            if (contentType == null)
            {
                inputEntity = new StringEntity(jsonMessage);
            }
            else if (charset == null)
            {
                inputEntity = new StringEntity(jsonMessage, ContentType.create(contentType));
            }
            else
            {
                inputEntity = new StringEntity(jsonMessage, ContentType.create(contentType, charset));
            }
            put.setEntity(inputEntity);
            
            /* PUT the message */
            /* If I'm authenticating, set up a context with a credentials provider to use */
            CloseableHttpResponse response;
            if (this.authUsername != null && this.authPassword != null)
            {
                /* First create the credentials provider */
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                String authHost = (this.authHost == null) ? AuthScope.ANY_HOST : this.authHost;
                int authPort = (this.authPort == null) ? AuthScope.ANY_PORT : Integer.decode(this.authPort);
                String authRealm = (this.authRealm == null) ? AuthScope.ANY_REALM : this.authRealm;
                Credentials creds;
                if (this.authDomain != null)
                {
                    creds = new NTCredentials(authUsername, authPassword, authWorkstation, authDomain);
                }
                else
                {
                    creds = new UsernamePasswordCredentials(authUsername, authPassword);
                }
                credsProvider.setCredentials(new AuthScope(authHost, authPort, authRealm), creds);
                /* Then create the context and attach the credentials provider to it */
                HttpClientContext context = HttpClientContext.create();
                context.setCredentialsProvider(credsProvider);
                /* Now execute the request using the context */
                response = client.execute(put, context);
            }
            else
            {
                response = client.execute(put);
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
    
    /* Output properties from JSON put */
    
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
