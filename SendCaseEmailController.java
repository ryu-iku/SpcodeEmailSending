/*
2017/3/9 16:53 4 attachments OK.
** Class:  SendCaseEmailController
** Description: Controller for the SendCaseEmail custom Visual Force page. 
*/
public with sharing class SendCaseEmailController {

    public String addlRecipients {get; set;}
    public Case   ourCase {get; set;}
    public EmailMessage emailMsg {get; private set;}

    private OrgWideEmailAddress sender = null;

    private static final String SUPPORT_EMAIL_ADDRESS = 'support@somewhere.com';
    private static final String SUPPORT_NAME = 'Support Email';

    public SendCaseEmailController(ApexPages.StandardController controller) {
        ourCase = (Case)controller.getRecord();

        // create our EmailMessage 
        emailMsg = new EmailMessage();

        // get our org-wide email address to set from/sender field
        sender = [select Id from OrgWideEmailAddress where DisplayName = 'フフルル'];
    }

    public Attachment attachment {
        get {
            if (attachment==null) {
                System.debug('==========> creating new empty attachment.');
                attachment = new Attachment();
            }
            return attachment;
        }
        set;
    }
    
    
    public Attachment attachment01 {
        get {
            if (attachment01==null) {
                System.debug('==========> creating new empty attachment01.');
                attachment01 = new Attachment();
            }
            return attachment01;
        }
        set;
    }

    public Attachment attachment02 {
        get {
            if (attachment02==null) {
                System.debug('==========> creating new empty attachment01.');
                attachment02 = new Attachment();
            }
            return attachment02;
        }
        set;
    }

    public Attachment attachment03 {
        get {
            if (attachment03==null) {
                System.debug('==========> creating new empty attachment03.');
                attachment03 = new Attachment();
            }
            return attachment03;
        }
        set;
    }

    // send email message per the attributes specified by user.
    public PageReference send() {
        try {
            // now create our SingleEmailMessage to send out.
            Messaging.SingleEmailMessage singleEmailMsg = new Messaging.SingleEmailMessage();

            // concatenate all Bcc Addresses
            if (emailMsg.BccAddress != null && emailMsg.BccAddress != '') {
                singleEmailMsg.setBccAddresses(emailMsg.BccAddress.split(';'));
            }

            // concatenate all CC Addresses
            if (emailMsg.CcAddress != null && emailMsg.CcAddress != '') {
                singleEmailMsg.setCcAddresses(emailMsg.CcAddress.split(';'));
            }
            singleEmailMsg.setOrgWideEmailAddressId(sender.Id); 

            singleEmailMsg.setSubject(emailMsg.Subject);
            singleEmailMsg.setPlainTextBody(emailMsg.TextBody);

            // now add additional recipients
            String[] addlToAddresses = null;
            if (addlRecipients != null && addlRecipients != '') {
                addlToAddresses = addlRecipients.split(';');
            }
            // now lets add any additional recipients to our list of recipients.
            List<String> lstToAddresses = null;
            if (addlToAddresses != null) {
                // now append these to our main recipient.
                lstToAddresses = new List<String>(addlToAddresses);
            } else {
                lstToAddresses = new List<String>();
            }
            lstToAddresses.add(emailMsg.ToAddress);
            singleEmailMsg.setToAddresses(lstToAddresses); 

            // now we need to reset the ToAddress for our EmailMessage.
            emailMsg.ToAddress += (addlRecipients != null ? ';' + addlRecipients : '');
            
            System.debug('attachment exist' + (attachment!=null));
            
            if (attachment.body == null) {
                System.debug('Let us install attachment object!');
                attachment.Body = EncodingUtil.base64Decode('UEsDBBQAAAAAAJyKVUqjvzE8CgAAAAoAAAAKAAAAc3Bjb2RlLnR4dGFiYzEyMzQ1NjdQSwECFAAUAAAAAACcilVKo78xPAoAAAAKAAAACgAAAAAAAAABACAAAAAAAAAAc3Bjb2RlLnR4dFBLBQYAAAAAAQABADgAAAAyAAAAAAA=');
                attachment.ContentType = 'application/zip';
                attachment.Name = 'SPCode0306.zip';
            }else{System.debug('the name of attachment is' + attachment.name);}
            
            // now attach file to email if there is one. Have to check the Body as Attachment
            // itself will never be null as it is always created first time it is accessed.
            
            System.debug('start attaching');
            System.debug('the name of attachment is ' + attachment.name);
            System.debug('the name of attachment01 is ' + attachment01.name);
            System.debug('the name of attachment02 is ' + attachment02.name);
            System.debug('the name of attachment03 is ' + attachment03.name);

            List<Messaging.EmailFileAttachment> emailAttachmentList = new List<Messaging.EmailFileAttachment>();
            for (Attachment attachmentEle: new List<Attachment>{attachment,attachment01,attachment02,attachment03}){
                if (attachmentEle != null) {
                    Messaging.EmailFileAttachment emailAttachment = new Messaging.EmailFileAttachment();
                    emailAttachment.setBody(attachmentEle.Body);
                    emailAttachment.setFileName(attachmentEle.Name);
                    emailAttachmentList.add(emailAttachment);
                }
            }
            singleEmailMsg.setFileAttachments(emailAttachmentList);
        }
        catch (Exception e) {
            // on failure, display error message on existing page so return null to return there.
            String errorMsg = 'Exception thrown trying to send Email Message. Details = ' + e;
            System.debug('==========> ' + errorMsg);
            ApexPages.addMessage(new ApexPages.Message(ApexPages.Severity.ERROR, errorMsg));
            return null;
        }

        return null;
    }

    // cancel creation of emailMessage. 
    public PageReference cancel() {
        // no need to do anything - just return to calling page.
        PageReference pgRef = new PageReference('/' + ourCase.Id);
        pgRef.setRedirect(true);
        return pgRef;
    }

    // populate our Email Message Template from the database, filling merge fields with
    // values from our Case object. Then store resulting template in EmailMessage for
    // display to end-user who is free to edit it before sending.
    public PageReference populateTemplate() {
        // we need to perform the merge for this email template before displaying to end-user.

        EmailTemplate emailTemplate = [select Body, HtmlValue, Subject, DeveloperName, BrandTemplateId 
            from EmailTemplate where DeveloperName='SPCodeMail' limit 1];

        // construct dummy email to have Salesforce merge BrandTemplate (HTML letterhead) with our email
        Messaging.SingleEmailMessage dummyEmailMsg = new Messaging.SingleEmailMessage();
        dummyEmailMsg.setTemplateId(emailTemplate.Id);
        // This ensures that sending this email is not saved as an activity for the targetObjectId. 
        dummyEmailMsg.setSaveAsActivity(false);

        // send dummy email to populate HTML letterhead in our EmailMessage object's html body.
        String[] toAddresses = new String[]{'yliu@netprotections.co.jp'};
        dummyEmailMsg.setToAddresses(toAddresses);
        dummyEmailMsg.setReplyTo(SUPPORT_EMAIL_ADDRESS);
        
        // now send email and then roll it back but invocation of sendEmail() 
        // means merge of letterhead & body is done

        // TargetObject is User. This tells the emailMsg to use the email message
        // associated with our dummy User. This is necessary so we can populate our
        // email message body & subject with merge fields from template
        Savepoint sp = Database.setSavepoint();


        dummyEmailMsg.setTargetObjectId('003O000000zTek1IAC');
        dummyEmailMsg.setWhatId(ourCase.Id);
        Messaging.sendEmail(new Messaging.SingleEmailMessage[] {dummyEmailMsg});
        // now rollback our changes.
        Database.rollback(sp);

        // now populate our fields with values from SingleEmailMessage.
        //emailMsg.BccAddress  = UserInfo.getUserEmail();
        //emailMsg.Subject     = dummyEmailMsg.getSubject();
        emailMsg.Subject     = ApexPages.currentPage().getParameters().get('sub');
        emailMsg.TextBody    = dummyEmailMsg.getPlainTextBody();
        emailMsg.ToAddress   = dummyEmailMsg.getToAddresses().get(0);
        emailMsg.FromAddress = SUPPORT_EMAIL_ADDRESS; 
        emailMsg.CcAddress   = '';
        emailMsg.ParentId    = ourCase.Id;
        return null;
    }
}