package com.smart.emailservice;

import java.util.Properties;



import org.springframework.stereotype.Component;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


@Component
public class EmailService {

public boolean sendEmail(String message,String subject,String to) {
		
		boolean f=false; 
		
		String from ="karanlearn789@gmail.com";
		
		// variable for gmail
				String host="smtp.gmail.com";
				
				// get yhe system properties
				Properties properties = System.getProperties();
				System.out.println("PROPERTIES "+ properties);
				
				// setting important information to properties object
				
				//host set
				properties.put("mail.smtp.host", host);
				properties.put("mail.smtp.port", "465");
				properties.put("mail.smtp.ssl.enable", "true");
				properties.put("mail.smtp.auth", "true");
				
				//Step 1: to get the session object
				
				Session session = Session.getInstance(properties, new Authenticator() {

					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						
						return new PasswordAuthentication("karanlearn789@gmail.com","qkzj gmfc nvmb rvex");
					}
					
					
				});
				session.setDebug(true);
				
				// Step 2 : compose the message [text,multi media]
				 MimeMessage m = new MimeMessage(session);
				 
				 try {
				// from email
				 m.setFrom(from);
				 
				 // adding recipient to 
				 m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
				 
				 // adding subject to message
				 m.setSubject(subject);
				 
				 // adding text to message
				// m.setText(message);
				 m.setContent(message,"text/html");

				 // Step 3: send the message using Transport class
				 Transport.send(m);
				 
				 f=true;
				 
				 System.out.println("send successfully.....");
				 }catch (Exception e) {
					e.printStackTrace();
				}
				 
				 return f;
		
	}
	
}
