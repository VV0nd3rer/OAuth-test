package app;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import org.apache.http.client.AuthCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import app.domain.Email;
import sun.net.www.protocol.http.AuthCacheImpl;
import sun.net.www.protocol.http.AuthCacheValue;

import static javax.mail.Message.RecipientType;
/**
 * Created by Liudmyla Melnychuk on 13.2.2019.
 */
public class Oauth {
    private static final Logger logger = LoggerFactory.getLogger(Oauth.class);
    private static final String GOOGLE_CREDENTIALS_FILE_PATH = "/diary.json";
    private static final String APPLICATION_NAME = "Diary";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();


    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        //props.put("mail.smtp.auth", "true");//Outgoing server requires authentication
        props.put("mail.smtp.starttls.enable", "true");//TLS must be activated
        //props.put("mail.smtp.host", "smtp.gmail.com"); //Outgoing server (SMTP) - change it to your SMTP server
        //props.put("mail.smtp.port", "587");//Outgoing port
        //props.put("mail.smtp.auth.mechanisms", "XOAUTH2");

        Session session = Session.getDefaultInstance(props);
        session.setDebug(true);
        return session;
    }

    public static void main(String[] args) {
        Oauth oauth = new Oauth();
        oauth.sendGMAILEmail(new Email("kverchi24@gmail.com", "hi", "test"));
    }

    private GoogleCredential getGoogleSpecificCredentials(final NetHttpTransport HTTP_TRANSPORT,
                                                          final JsonFactory JSON_FACTORY) {
        GoogleCredential googleCredential = null;
        List<String> scopes = new ArrayList<>();
//        scopes.add("https://mail.google.com/");
scopes.add(GmailScopes.MAIL_GOOGLE_COM);
        try(InputStream in = Oauth.class.getResourceAsStream(GOOGLE_CREDENTIALS_FILE_PATH)) {
            GoogleCredential credential = GoogleCredential.fromStream(in, HTTP_TRANSPORT,  JSON_FACTORY)
                    .createScoped(scopes);
            googleCredential = new GoogleCredential.Builder()
                    .setTransport(credential.getTransport())
                    .setJsonFactory(credential.getJsonFactory())
                    .setServiceAccountProjectId(credential.getServiceAccountProjectId())
                    .setServiceAccountPrivateKeyId(credential.getServiceAccountPrivateKeyId())
                    .setServiceAccountId(credential.getServiceAccountId())
                    /*.setServiceAccountPrivateKeyFromP12File(
                            new File(GmailEmailServiceImpl.class.getResource(GOOGLE_CREDENTIALS_FILE_PATH_P12).getFile()))*/
                    .setServiceAccountUser("diaryofkverchi@gmail.com")
                    .setServiceAccountPrivateKey(credential.getServiceAccountPrivateKey())
                    .setServiceAccountScopes(credential.getServiceAccountScopes())
                    .build();
            //logger.info("Refresh token: {} ", googleCredential.refreshToken());
            /*credential.refreshToken();
            googleCredential.setRefreshToken(credential.getRefreshToken());
            googleCredential.setAccessToken(credential.getAccessToken());*/
            //googleCredential.refreshToken();
        } catch (IOException e) {
            logger.error("IOException ", e);
        } /*catch (GeneralSecurityException e) {
            e.printStackTrace();
            logger.error("GeneralSecurityException ", e);
        }*/
        return googleCredential;
    }
    private MimeMessage createMimeMessage(Email email) {
        MimeMessage message = new MimeMessage(createSession());

        try {
            message.setFrom(new InternetAddress("diaryofkverchi@gmail.com"));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(email.getRecipientAddress()));
            message.setSubject(email.getSubject());
            message.setContent(email.getText(), "text/html");
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return message;
    }
    private static Message createMessageWithEmail(MimeMessage email) {
        Message message = new Message();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            email.writeTo(baos);
            String encodedEmail = Base64.encodeBase64URLSafeString(baos.toByteArray());
            message.setRaw(encodedEmail);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return message;
    }
    private void sendGMAILEmail(Email email) {
        final NetHttpTransport HTTP_TRANSPORT;
        try {
            // GoogleNetHttpTransport is an utility for Google APIs based on NetHttpTransport.
            // newTrustedTransport() returns a new instance of NetHttpTransport that
            // uses GoogleUtils.getCertificateTrustStore() for the trusted certificates using NetHttpTransport
            // and  throws GeneralSecurityException, IOException
            AuthCacheValue.setAuthCache(new AuthCacheImpl());
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                    getGoogleSpecificCredentials(HTTP_TRANSPORT, JSON_FACTORY))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            MimeMessage mimeMessage = createMimeMessage(email);
            Message message = createMessageWithEmail(mimeMessage);
            message = service.users().messages().send("me", message).execute();

            System.out.println("Message id: " + message.getId());
            System.out.println(message.toPrettyString());
        } catch (GeneralSecurityException e) {
            logger.error("GeneralSecurityException ", e);
        } catch (IOException e) {
            logger.error("IOException ", e);
        }
    }
}
