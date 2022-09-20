package unitn.iehr.researchCentre;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
public class ResearchCentreController {
/*
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }
*/
    @Autowired
    ResearchCentreModel researchCentreModel;

    ObjectMapper mapper = new ObjectMapper();

    @ApiIgnore
    @GetMapping("/")
    public String index() {
        return "Research Centre Node";
    }

    @ApiIgnore
    @PostMapping (
            value = "/sendEnrollmentConsent",
            consumes = "text/plain"
    )
    public @ResponseBody ResponseEntity<Object> sendEnrollmentConsent(
            @RequestParam(value = "studyId", defaultValue = "studyId") String studyId,
            @RequestBody String body
    ) throws
            JSONException,
            CertificateException,
            NoSuchAlgorithmException,
            IOException,
            SignatureException,
            InvalidKeyException,
            UnrecoverableKeyException,
            KeyStoreException
    {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody = new JSONObject(body);
        }catch (JSONException err){
            System.out.println("JSON conversion error:"+err);
        }

       String citizenConsentEnc = jsonBody.getString("consent");
       String citizenSignatureEnc = jsonBody.getString("signature");
       byte[] citizenConsent = Base64.getDecoder().decode(citizenConsentEnc);
       byte[] citizenSignature = Base64.getDecoder().decode(citizenSignatureEnc);
       String citizenCertificateDataString = jsonBody.getString("citizen_certificate");

       String citizenPseudo = jsonBody.getString("citizen_pseudo");
       String enrollmentCriteria = jsonBody.getString("enrollment_criteria");
       String sehrAppId = jsonBody.getString("sehrapp_id");
       researchCentreModel.logger.info("sendEnrollmentConsent request for studyID:"+studyId+" by sehr-app:"+sehrAppId);


        //String citizenConsentEnc = "eyJyZXNvdXJjZVR5cGUiOiJDb25zZW50IiwidGV4dCI6eyJzdGF0dXMiOiJnZW5lcmF0ZWQiLCJkaXYiOiI8ZGl2IHhtbG5zPVwiaHR0cDovL3d3dy53My5vcmcvMTk5OS94aHRtbFwiPkkgaGF2ZSByZWFkIGFuZCB1bmRlcnN0b29kIEludGVyb3BFSFJhdGUncyA8YSBocmVmPVwibnVsbFwiPlByaXZhY3kgUG9saWN5PC9hPi5cXG5cXG5JIGhlcmVieSBnaXZlIHBlcm1pc3Npb24gdG8gc2hhcmUgaGVhbHRoIGRhdGEgdG8gcmVmZXJlbmNlIHJlc2VhcmNoIGNlbnRlciB0byBwcm9jZXNzICh2aWV3LCBzdG9yZSwgZWRpdCBldGMuKSB0aGUgcGVyc29uYWwgZGF0YSBzdG9yZWQgaW4gbXkgUGVyc29uYWwgSGVhbHRoIFJlY29yZCBvbiB0aGlzIGFwcGxpY2F0aW9uIGZvciB0aGUgcHVycG9zZSBvZiByZXNlYXJjaC4gSSB1bmRlcnN0YW5kIHRoYXQgbXkgY29uc2VudCB3aWxsIHJlbWFpbiB2YWxpZCBmb3IgdGhlc2UgcHVycG9zZXMgdW5sZXNzIEkgYWZmaXJtYXRpdmVseSB3aXRoZHJhdyBpdC4gSSBoYXZlIHRoZSByaWdodCB0byB3aXRoZHJhdyB0aGlzIGNvbnNlbnQgYXQgYW55IHRpbWUuPC9kaXY+In0sInN0YXR1cyI6ImFjdGl2ZSIsImRhdGVUaW1lIjoiMjAyMi0wMi0yM1QwOTo1MjoyMiswMjowMCIsInBlcmZvcm1lciI6W3sicmVmZXJlbmNlIjoiUmVmZXJlbmNlIFJlc2VhcmNoIENlbnRlciJ9XSwib3JnYW5pemF0aW9uIjpbeyJyZWZlcmVuY2UiOiJSZWZlcmVuY2UgUmVzZWFyY2ggQ2VudGVyIn1dfQ==";
        //String citizenSignatureEnc = "RUl3S1NBNVdKNFM2a0cxVjZGT21FTlYzN0JyY0VPNU9LcjB2UjdHS1FpVWVqb0NTUGlFYWw3ZzJYVzZxSmtrSGltNDRJcTlQbkM5ODVMcEx0R3JPMHF3NVFmeFE1d0V0VjBnZUpOR2hmOG85aE0yMVh0SkFxTFRBREdEUXNZOU1INU1UY0ZrS2xaMTBNM1RqandydGtVSDhzY1ZIWEd6UEpHWDFwTGFvaGhkSHJoaEMwU0I3Zy9kYzFTMjdGejlYMmN5YXdlMktQbjY0L1dqaVhEZ04yc2FXMkdkR05oVWdUZE44ZWhhN1ZFczFrM0xOaEk4M001dFJmeklwZFZSZGlyTjlORXk3K3llTy9GTFIvNEtvUDROQnlxQXkvcE9CRXRKakM5SGx0UmxLQXd0bmduajBlUnBVeWtJVDlrNGhqamJoZ0haV004ZXRzR3VLR1NZSUVnPT0";



        boolean verification = researchCentreModel.verifyConsent(citizenCertificateDataString, citizenConsent, citizenSignature, sehrAppId ,studyId);
        JSONObject jsonResponseBody;
        if(verification){
            if(!researchCentreModel.participantEnrolledInStudy(citizenPseudo, studyId)) {
                Date date = new Date(); // This object contains the current date value
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                System.out.println(formatter.format(date).toString());
                researchCentreModel.addParticipant(sehrAppId, citizenPseudo, studyId, formatter.format(date).toString());
            }

            jsonResponseBody = researchCentreModel.signConsent(Base64.getEncoder().encodeToString(citizenConsent), studyId, sehrAppId);
            System.out.println("COUNTER SIGNED CONSENT:"+ jsonResponseBody);
            return new ResponseEntity<Object>(jsonResponseBody.toString(), HttpStatus.OK);
        }
        jsonResponseBody = new JSONObject("{\"error\":\"Certificate Verification Failure\"}");
        return new ResponseEntity<Object>(jsonResponseBody.toString(), HttpStatus.NOT_ACCEPTABLE);
    }

    //@ApiIgnore
    @PostMapping("/sendExitNotification")
    public @ResponseBody ResponseEntity<Object> sendExitNotification(
            @RequestParam(value = "studyId", defaultValue = "") String studyId,
            @RequestParam(value = "citizenPseudo", defaultValue = "") String citizenPseudo,
            @RequestParam(value = "reason", defaultValue = "") String reason,
            @RequestParam(value = "reasonText", defaultValue = "") String reasonText,
            @RequestParam(value = "citizenSignature", defaultValue = "") String citizenSignature
    ) throws JsonProcessingException {
        return new ResponseEntity<Object>(researchCentreModel.sendExitNotification(citizenPseudo, studyId, reasonText), HttpStatus.OK);
    }

//This should be included in sendExitNotification (as a special case of exit from a study)
    @ApiIgnore
    @GetMapping("/sendWithdrawal")
    public String sendWithdrawal(
            @RequestParam(value = "studyId", defaultValue = "studyId") String studyId,
            @RequestParam(value = "citizenPseudo", defaultValue = "citizenPseudo") String citizenPseudo,
            @RequestParam(value = "citizenSignature", defaultValue = "citizenSignature") String citizenSignature
    ){
        return researchCentreModel.sendWithdrawal(studyId, citizenPseudo, citizenSignature);
    }

    @ApiIgnore
    @PostMapping("/sendHealthData")
    public @ResponseBody ResponseEntity<Object> sendHealthData(
            @RequestParam(value = "studyId", defaultValue = "World") String studyId,
            @RequestParam(value = "citizenPseudo", defaultValue = "World") String citizenPseudo,
            @RequestParam(value = "pkey", defaultValue = "World") String pkey,
            @RequestBody String body
    ) throws Exception {


        JSONObject jsonResponseBody = researchCentreModel.storeHealthData(body, pkey, studyId, citizenPseudo);
        if(jsonResponseBody.getString("status").equals("ok")){
            return new ResponseEntity<Object>(HttpStatus.OK);
        } else {
            return new ResponseEntity<Object>(jsonResponseBody.getString("message"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/retrievePseudoIdentity")
    public String retrievePseudoIdentity(
            @RequestParam(value = "studyId", defaultValue = "World") String studyId
    ) throws IOException {
        return researchCentreModel.retrievePseudoIdentity(studyId);
    }

    /* APIs out of RDS Protocol */

    @GetMapping("/getParticipantsByStudy")
    public @ResponseBody String getParticipants(
            @RequestParam(value = "studyID", defaultValue = "studyID") String studyID,
            @RequestHeader String apikey
    ) throws IOException {
        return researchCentreModel.getParticipants(studyID, apikey);
    }

    @GetMapping(value = "/getHealthDataByStudyAndParticipant", produces="application/json")
    public @ResponseBody String getHelthDataByStudy(
            @RequestParam(value = "studyId", defaultValue = "studyID") String studyID,
            @RequestParam(value = "participant", defaultValue = "participant") String participant,
            @RequestHeader String apikey
            //@RequestParam(value = "token", defaultValue = "null") String token
    ) throws IOException, JSONException {
        return researchCentreModel.getHealthDataByStudy(studyID, participant, apikey).toString();
    }
}
