package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entites.Contact;
import com.smart.entites.MyOrder;
import com.smart.entites.User;
import com.start.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myOrderRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String username = principal.getName();
		System.out.println(username);

		// get user by using username(Email)
		User user = userRepository.getUserByUserName(username);
		System.out.println(user);

		model.addAttribute("user", user);
	}

	// dashboard home
	@GetMapping("/index")
	public String dashBoard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processsContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {
		try {

			String name = principal.getName();
			User user = userRepository.getUserByUserName(name);

			if (file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is empty");
				contact.setImage("default.png");
			} else {
				// file the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/images").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Images is uploaded");
			}

			contact.setUser(user);
			user.getContact().add(contact);
			this.userRepository.save(user);

			System.out.println(contact);
			System.out.println("Contact added done");

			// message success
			session.setAttribute("message", new Message("Contact is added ! Add more", "success"));

		} catch (Exception e) {
			System.out.println("Error " + e.getMessage());
			e.printStackTrace();
			// message failed
			session.setAttribute("message", new Message("Something went wrong !! Try again", "danger"));

		}
		return "normal/add_contact_form";
	}

	// per page =5[n]
	// current page =0 [page]

	// show contact here
	@GetMapping("/show-contacts/{page}")
	public String showContact(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "View Contact");

		// contact ki list ko bejna hai

		String email = principal.getName();
		User user = this.userRepository.getUserByUserName(email);

		Pageable pageable = PageRequest.of(page, 7);

		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);

		model.addAttribute("totalPages", contacts.getTotalPages());

		/*
		 * String email = principal.getName(); User user =
		 * this.userRepository.getUserByUserName(email); List<Contact> list =
		 * user.getContact(); System.out.println(list);
		 * System.out.println("Contact list hai");
		 */
		return "normal/show_contacts";
	}

	// showing particular contact details
	@GetMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		System.out.println(cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		System.out.println("-------------");

		Contact contact = contactOptional.get();

		String email = principal.getName();
		User user = this.userRepository.getUserByUserName(email);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_details";
	}

	// delete user
	@GetMapping("/delete/{cId}")
	@Transactional
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session,
			Principal principal) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		System.out.println("contact " + contact.getcId());

		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContact().remove(contact);

		this.userRepository.save(user);

		// contact.setUser(null);
		// this.contactRepository.delete(contact);

		session.setAttribute("message", new Message("Contact deleted successfully", "success"));

		System.out.println("delete successfully");

		return "redirect:/user/show-contacts/0";
	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateContact(@PathVariable("cid") Integer cid, Model model) {

		Contact contact = this.contactRepository.findById(cid).get();

		System.out.println(contact.getName());

		model.addAttribute("contact", contact);
		model.addAttribute("title", "Update Contact");

		return "normal/update_form";
	}

	// update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model model, HttpSession session, Principal principal) {

		try {
			// image
			// old contact details
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();

			if (!file.isEmpty()) {
				// file work
				// rewrite

				// delete old photo
				File deleteFile = new ClassPathResource("static/images").getFile();
				File file1 = new File(deleteFile, oldcontactDetail.getImage());
				file1.delete();

				// update new photo

				File saveFile = new ClassPathResource("static/images").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldcontactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);

			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated..", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(contact.getName());
		System.out.println(contact.getcId());

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {

		model.addAttribute("title", "Profile");

		return "normal/profile";
	}

	// open setting handler
	@GetMapping("/settings")
	public String openSetting() {
		return "normal/settings";
	}

	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession httpSession) {

		System.out.println("----------Password--------");
		System.out.println("OLD PASSWORD " + oldPassword);
		System.out.println("NEW PASSWORD " + newPassword);

		String email = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(email);
		System.out.println(currentUser.getPassword());

		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			// change the paswword

			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);

			httpSession.setAttribute("message", new Message("Your password is successfully change.", "success"));

		} else {
			// error
			httpSession.setAttribute("message",
					new Message("Your old password is wrong please enter correct old password.", "danger"));

			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

// create order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String,Object> data, Principal principal) throws RazorpayException {

		System.out.println("Hey Order function example..");
		System.out.println(data);
		
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client = new RazorpayClient("rzp_test_SbdlkgADXw2gyj", "26lGZFIj9yK6hoGi3jG1E5Kq");

		JSONObject ob=new JSONObject();
		ob.put("amount",amt*100);
		ob.put("currency","INR");
		ob.put("receipt","txn_235425");
		
		// creating new order
		
		Order order=client.orders.create(ob);
		System.out.println(order);
		System.out.println("------");
		
		
		// save the order into database
		
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount"));
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);
		
		return order.toString();
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?> update_order(@RequestBody Map<String, Object> data) {
		
		MyOrder myorder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		
		myorder.setPaymentId(data.get("payment_id").toString());
		myorder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myorder);
		System.out.println(data);
		
		return ResponseEntity.ok(Map.of("msg","updated"));
	}

}
