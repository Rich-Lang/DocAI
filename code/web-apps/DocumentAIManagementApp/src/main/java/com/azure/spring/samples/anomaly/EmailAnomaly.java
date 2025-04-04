package com.azure.spring.samples.anomaly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.spring.samples.cosmosdb.CosmosDBOperation;
import com.azure.spring.samples.model.AttachmentData;
import com.azure.spring.samples.model.EmailData;
import com.azure.spring.samples.utils.Category;

public class EmailAnomaly {

    private static final String LIFE_INSURANCE_CATEGORY = "life-insurance";

	private static final String MEDICAL_LAB_REPORT = "medical-lab-report";

	private static Logger logger = LoggerFactory.getLogger(EmailAnomaly.class);
    
    private CosmosDBOperation cosmosDB;

	public EmailAnomaly(CosmosDBOperation cosmosDBOperation) {
		super();
		this.cosmosDB = cosmosDBOperation;
	}

	/**
	 * Find intent from email
	 * Check if necessary information has been provided
	 * 
	 * @param emailBodyId - id of item of message type email-body in CosmosDB
	 * @return
	 */
	public String checkIntentContentGap(String emailBodyId) {
		String sqlStatement = "SELECT * FROM EmailExtracts e WHERE e.id = '" + emailBodyId + "' " +
				" AND e.messageType IN ('email-body') ";
		
  		Iterator<EmailData> iterateED = cosmosDB.query(sqlStatement, EmailData.class);
  		String reviewMessage;
  		if (iterateED == null) {
  			cosmosDB.close();
  			reviewMessage = String.format("Intent Content Check: Did not find the email record for id %s in Cosmos DB", emailBodyId);
  			logger.info(reviewMessage);
  			return reviewMessage;
  		}
  		// There should be just one record for a given id
  		EmailData ed = iterateED.next();
  		String messageId = ed.getMessageId();
  		
  		List<String> emailCategories = Category.getCategoryList(ed.getCategories());
  		List<String> requestNewQuoteCategory = new ArrayList<>();
  		requestNewQuoteCategory.add("request-new-quote");
  		requestNewQuoteCategory.add("request-new-claim");
  		
  		// If intent is to ask for a new quote or file a new claim, then there has to be a matching 
  		// attachment/form, else nothing to check further
  		if (!Category.hasAny(requestNewQuoteCategory, emailCategories)) {
			cosmosDB.close();
			reviewMessage =  String.format("Intent Content Check: Intent of email needs no attached form or file.");
			logger.info(reviewMessage);
			return reviewMessage;
  		}
  		
		sqlStatement = "SELECT * FROM EmailExtracts e WHERE e.messageId IN ('" + messageId + "') " +
						"AND e.messageType IN ('email-attachment') ";
		Iterator<AttachmentData> iterateAD = cosmosDB.query(sqlStatement, AttachmentData.class);
		if (iterateAD == null) {
			cosmosDB.close();
			reviewMessage = String.format("Intent Content Check: Expected at least 1 attachment, found none for messageId %s", messageId);
			logger.info(reviewMessage);
			return reviewMessage;
		}
		String correspondingAttachmentName = null;
		while(iterateAD.hasNext()) {
			AttachmentData ad = iterateAD.next();
			List<String> attachmentCategories = Category.getCategoryList(ad.getCategories());
			for (String emailCategory : emailCategories) {
				for (String attachmentCategory : attachmentCategories) {
					if (attachmentCategory.contains(emailCategory)) {
						// Found category match
						// like for email::attachment the categories match
						// workers-compensation::workers-compensation-application
						correspondingAttachmentName = ad.getAttachmentName();
					} else if (emailCategory.contains(LIFE_INSURANCE_CATEGORY) && attachmentCategory.contains(MEDICAL_LAB_REPORT)) {
						correspondingAttachmentName = ad.getAttachmentName();
					}
				}
			}
		}
  		cosmosDB.close();
		if (correspondingAttachmentName != null) {
			reviewMessage = String.format("Intent Content Check: For the email's intent, got required attachment %s", 
											correspondingAttachmentName);
			logger.info(reviewMessage);
		} else {
			reviewMessage = String.format("Intent Content Check: For the email's intent, got NO required attachment(s)");
			logger.info(reviewMessage);
		}
		return reviewMessage;
	}
	
	/**
	 * Any PII data or content moderation needed? 
	 * 
	 * @return
	 */
	public String checkContentModeration(String emailBodyId) {
		return "Content Moderation Check: NotYetImplemented";
	}
	
}
