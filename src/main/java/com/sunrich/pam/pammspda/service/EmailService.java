package com.sunrich.pam.pammspda.service;

import com.sun.mail.util.MailSSLSocketFactory;
import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.EmailConfig;
import com.sunrich.pam.common.domain.EmailTemplate;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.NotFoundException;
import com.sunrich.pam.pammspda.repository.CustomerRepository;
import com.sunrich.pam.pammspda.repository.EmailConfigRepository;
import com.sunrich.pam.pammspda.repository.EmailTemplateRepository;
import com.sunrich.pam.pammspda.repository.PdaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@Service
public class EmailService {
  private PdaService pdaService;
  private PdaRepository pdaRepository;
  private EmailConfigRepository emailConfigRepository;
  private EmailTemplateRepository emailTemplateRepository;
  private CustomerRepository customerRepository;

  public EmailService(PdaService pdaService, PdaRepository pdaRepository, EmailConfigRepository emailConfigRepository, EmailTemplateRepository emailTemplateRepository, CustomerRepository customerRepository) {
    this.pdaService = pdaService;
    this.pdaRepository = pdaRepository;
    this.emailConfigRepository = emailConfigRepository;
    this.emailTemplateRepository = emailTemplateRepository;
    this.customerRepository = customerRepository;
  }

  /**
   * Used to create pdf and attach it as MimeBodyPart
   *
   * @param id -pda id
   * @return pdf as a MimeBodyPart
   * @throws Exception
   */
  public MimeBodyPart getPdfAttachment(Long id, Constants.PdaType type, Integer group, Boolean bothCurrency) throws Exception {
    MimeBodyPart pdfBodyPart = new MimeBodyPart();
    byte[] bytes = pdaService.generate(id, type, group, bothCurrency);
    DataSource dataSource = new ByteArrayDataSource(bytes, "application/pdf");
    pdfBodyPart.setDataHandler(new DataHandler(dataSource));
    pdfBodyPart.setFileName(pdaService.getFileName(id, type) + ".pdf");
    return pdfBodyPart;
  }

  /**
   * Used to send a mail to client with pdf as an attachment
   * @param id -pda identifier
   * @throws Exception
   */
  public void sendPda(Long id, Constants.PdaType type, Integer group, Boolean bothCurrency) throws Exception {
    Multipart multipart = new MimeMultipart();
    MimeBodyPart attachment = getPdfAttachment(id, type, group, bothCurrency);
    MimeBodyPart body = new MimeBodyPart();

    EmailTemplate emailTemplate = getMailBody();

    StringBuilder emailBody = new StringBuilder();
    emailBody = emailBody.append("<p>")
            .append(emailTemplate.getBody())
            .append("</p>")
            .append("<br />")
            .append(emailTemplate.getFooter());

    body.setContent(emailBody.toString(), "text/html");

    multipart.addBodyPart(body);
    multipart.addBodyPart(attachment);

    Long customerId = pdaRepository.findCustomerByIdAndRecordStatusTrue(id).getCustomer();
    String to = customerRepository.findCustomerEmailIdById(customerId);

    EmailConfig emailConfig = getEmailConfig();
    InternetAddress sender = new InternetAddress(emailConfig.getEmail());
    InternetAddress reciever = new InternetAddress(to);

    Session sessionAuthentication = setSessionAuthentication(emailConfig);

    MimeMessage message = new MimeMessage(sessionAuthentication);
    message.setFrom(sender);
    message.addRecipient(Message.RecipientType.TO, reciever);
    message.setSubject(emailTemplate.getSubject());
    message.setContent(multipart);

    Transport.send(message);
  }

  /**
   * Used to return session
   * @param emailConfig -EmailConfig object
   * @return session
   */
  private Session setSessionAuthentication(EmailConfig emailConfig) {

    Properties properties = new Properties();
    properties.setProperty("mail.user", emailConfig.getEmail());
    properties.setProperty("mail.password", emailConfig.getPassword());
    properties.put("mail.transport.protocol", "smtp");
    properties.put("mail.smtp.auth", "true");
    properties.put("mail.smtp.starttls.enable", "true");
    properties.put("mail.debug", "true");
    properties.put("mail.smtp.host", emailConfig.getHost());
    properties.put("mail.smtp.port", emailConfig.getSmtp());
    MailSSLSocketFactory mailSSLSocketFactory = null;
    try {
      mailSSLSocketFactory = new MailSSLSocketFactory();
    } catch (GeneralSecurityException e1) {
      log.error("GeneralSecurityException while creating socket factory class: ", e1);
    }
    mailSSLSocketFactory.setTrustAllHosts(true);
    properties.put("mail.smtp.ssl.socketFactory", mailSSLSocketFactory);
    return Session.getInstance(properties, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(emailConfig.getEmail(), emailConfig.getPassword());
      }
    });
  }

  /**
   * Used to get email configuration such as port,host etc
   * @return EmailConfig object
   */
  private EmailConfig getEmailConfig() {
    Optional<EmailConfig> emailConfig = emailConfigRepository.findByIdAndRecordStatusTrue(1L);
    if (!emailConfig.isPresent()) {
      throw new NotFoundException(ErrorCodes.EMAIL_CONFIG_NOT_FOUND, "Email Configuration Not Found");
    }
    return emailConfig.get();
  }

  /**
   * Used to get email Body
   * @return EmailTemplate Object`
   * @throws MessagingException
   */
  private EmailTemplate getMailBody() throws MessagingException {
    Optional<EmailTemplate> emailTemplate = emailTemplateRepository.findByCodeAndRecordStatusTrue("PDA_MAILER_CLIENT");
    if (!emailTemplate.isPresent()) {
      throw new NotFoundException(ErrorCodes.EMAIL_TEMPLATE_NOT_FOUND, "Email Template  Not Found");
    }
    return emailTemplate.get();
  }
}
