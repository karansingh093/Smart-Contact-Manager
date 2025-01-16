package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.emailservice.EmailService;
import com.smart.entites.User;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {

	Random random = new Random(1000);
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EmailService emailService;
	
	// open email id form open handler
	
	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		
		System.out.println("Email "+email);
		
		// generating OTP of 4 digit
		
		
		int otp = random.nextInt(999999);
		
		System.out.println("OTP "+otp);
		
		String message=""
			       + "<div style='border:1px solid #e2e2e2; padding:20px'>"
			       + "<h1>"
			       + "OTP is "
			       + "<b>"+otp
			       + "</n>"
			       + "</h1>"
			       + "</div>";
		String subject=" FROM SCM";
		String to=email;
		
		boolean flag = this.emailService.sendEmail(message, subject, to);
		System.out.println(flag);
		
		if(flag){
			session.setAttribute("myotp",otp);
			session.setAttribute("email", email);
			return "verify_otp";
		
		}else {
			session.setAttribute("message","Check your email id !!");
			return "forgot_email";
		}
			
	}
	
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp,HttpSession session) {
		
		int myotp=(int)session.getAttribute("myotp");
		String email=(String) session.getAttribute("email");
		
		if(myotp==otp) {
			// password change
			
			User user = this.userRepository.getUserByUserName(email);
			
			if(user==null) {
				// send error mesasage
				session.setAttribute("message","User does not exist with this email!!");
				return "forgot_email";

			}else {
				// send change password
			}
			
			return "password_change_form";
		}
		else {
			session.setAttribute("message","You have entered wrong otp!!");
			return "verify_otp";
		}
		
	}
	
	// change password
	@PostMapping("/change-password")
	public String changePasssword(@RequestParam("newpassword") String newpassword,HttpSession session) {
		
		String email=(String) session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
		this.userRepository.save(user);
		
		return "redirect:/signin?change=password changed successfully";
	}
}




















