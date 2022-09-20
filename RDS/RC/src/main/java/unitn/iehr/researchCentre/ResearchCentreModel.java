package unitn.iehr.researchCentre;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import iehr.security.CryptoManagementFactory;
import iehr.security.api.CryptoManagement;
import org.springframework.stereotype.Component;
import unitn.iehr.researchCentre.mongoDB.Participant;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.sql.Timestamp;

import static java.util.Objects.isNull;

@Component
public class ResearchCentreModel {

    //OLD
    //public static final String CA_URL = "http://212.101.173.84:8071";
    public static final String CA_URL = "http://interoperate-ejbca-service.euprojects.net";


    private static CryptoManagement cryptoManagement = CryptoManagementFactory.create(CA_URL);
    private KeyPair RCKpair = cryptoManagement.aliceInitKeyPair();
    private KeyAgreement RCKpairKA = cryptoManagement.aliceKeyAgreement(RCKpair);
    private byte[] RCPubKeyEnc = cryptoManagement.alicePubKeyEnc(RCKpair);
    PublicKey alicePubKey = RCKpair.getPublic();


    private static Date date = new Date();
    public static final Logger logger = Logger.getLogger("RDS-logger");
    FileHandler fh;

    @Autowired
    private static DatabaseManager dbManager;
    private static ObjectMapper mapper = new ObjectMapper();

    public ResearchCentreModel(DatabaseManager dbManager) throws Exception {
        try {
            // This block configure the logger with handler and formatter
            try {
                Long datetime = System.currentTimeMillis();
                fh = new FileHandler("./log/RC-"+new Timestamp(datetime).toString()+"-log.log");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // the following statement is used to log any messages
            //logger.info("Research Centre Application is running");

        } catch (SecurityException e) {
            e.printStackTrace();
        }
        this.dbManager = dbManager;
    }

    public static String sendWithdrawal(String studyId, String citizenPseudo, String citizenSignature){
        logger.info("sendWithdrawal request for pseudoId:"+citizenPseudo+" and studyID:"+studyId);
        return "sendWithdrawal - " + studyId + " - " + citizenPseudo + " - " + citizenSignature;
    }

    public static String retrievePseudoIdentity(String studyId) throws IOException {
        logger.info("retrievePseudoIdentity request for studyID:"+studyId);
        //[GET] URL:8080/pseudo_identity with a URL param named prefix , which represents a research's unique ID.
        //make GET request

        URL url = new URL("http://pseudoid-generator-container:8080/pseudo_identity?prefix="+studyId);
        // when deploy on other machine use the ip of the remote machine
        //FTGM
        //URL url = new URL("http://10.97.32.223:8080/pseudo_identity?prefix="+studyId);
        //CHU
        //URL url = new URL("http://139.165.99.12:8080/pseudo_identity?prefix="+studyId);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        int status = con.getResponseCode();
        System.out.println("generetor call status:"+status);

        //read the response

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }


    public static boolean verifyConsent(String citizenCertificateData, byte [] citizenConsent, byte [] citizenSignature, String sehrAppId, String studyId) throws
            CertificateException,
            NoSuchAlgorithmException,
            InvalidKeyException
    {
        RSAPublicKey rsaPublicKey;
        try {
            byte[] certificateData = Base64.getMimeDecoder().decode(citizenCertificateData);
            X509Certificate certificate = cryptoManagement.toX509Certificate(certificateData);
            rsaPublicKey = (RSAPublicKey)certificate.getPublicKey();
        } catch (Exception e){
            logger.info("ERROR on verifyConsent request for studyID:"+studyId+" by sehr-app:"+sehrAppId+" - Exception:"+e.toString());
            return  false;
        }

        Boolean verify = null;
        try {
            verify = cryptoManagement.verifyPayload(rsaPublicKey, citizenConsent, citizenSignature);
        } catch (SignatureException e) {
            e.printStackTrace();
            System.out.println("ERROR on verifyConsent request for studyID:"+studyId+" by sehr-app:"+sehrAppId+" - Having result:"+verify);
            logger.info("ERROR on verifyConsent request for studyID:"+studyId+" by sehr-app:"+sehrAppId+" - Having result:"+verify);
            return false;
        }
        System.out.println("Check if user certificate is valid: "+ verify);
        logger.info("verifyCertificate request for studyID:"+studyId+" by sehr-app:"+sehrAppId+" - Having result:"+verify);
        return verify;
    }

    public static JSONObject signConsent(String citizenConsent, String sehrAppId, String studyId) throws
            UnrecoverableKeyException,
            KeyStoreException,
            InvalidKeyException,
            IOException,
            CertificateException,
            NoSuchAlgorithmException,
            SignatureException,
            JSONException {

        logger.info("signConsent request for studyID:"+studyId+" by sehr-app:"+sehrAppId);
        PrivateKey researchPrivateKey = cryptoManagement.getPrivateKey();
        String rcSignedConsent = cryptoManagement.signPayload(citizenConsent,researchPrivateKey);

/*
        byte[] rcCertificateData;
        try{
            rcCertificateData = cryptoManagement.getUserCertificate("research");
        } catch (Exception e){
            System.out.println("Internal Exception getUserCertificate: "+e.toString());
            rcCertificateData =  new byte [0];
        }
        String rcCertificateString = new String(rcCertificateData);
        //System.out.println("Sign Consent" + rcSignedConsent);
*/
        JSONObject jsonResponseBody = new JSONObject();
        jsonResponseBody.put("signed-contract", rcSignedConsent);
        jsonResponseBody.put("certificate", "null");
//        jsonResponseBody.put("certificate", rcCertificateString);
        return jsonResponseBody;
    }

    public JSONObject storeHealthData(String encryptedBundle, String pkey, String studyId, String citizenPseudo) throws Exception {

        //Check if citizenPseudo is correct for the given studyId
        List<Participant> participants = dbManager.getParticipants(studyId);
        boolean verified = false;
        for(Participant p : participants){
            if (citizenPseudo.equals(p.getPseudo())) verified = true;
        }

        if(verified) {
            // data decryption
            String symmetric = "Bos0HSxY4HWrVwEZaoywbAnP8a0BWExEfl5pyHULEXQ=";
            JSONObject encryptedBody = new JSONObject(encryptedBundle);

            //System.out.println("Symmetric:"+symmetric);
            //System.out.println("BODY:"+encryptedBody.getString("health_data"));

            String decrypted = null;
            try {
                decrypted = cryptoManagement.decrypt(encryptedBody.getString("health_data"), symmetric);
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject response = new JSONObject("{\"status\":\"error\",\"message\":\"error in health data decryption\"}");
                logger.info("storeHealthData request log: Error in data decryption");
                return response;
            }
            logger.info("storeHealthData request log: Decryption completed");
            //System.out.println("Decrypted: " + decrypted);


            // store health data

            SimpleDateFormat date = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
            String timeStamp = date.format(new Date());

            try {
                File dir = new File("./research-health-data/"+studyId+"/"+citizenPseudo);
                if(!dir.exists()){
                    if (!dir.exists()) dir.mkdirs();
                    System.out.println("New citizen folder created");
                    logger.info("storeHealthData request log: New citizen folder created");
                }
                File file = new File("./research-health-data/"+studyId+"/"+citizenPseudo+"/"+timeStamp+".json");
                if(file.createNewFile()){
                    System.out.println("File created: " + file.getName());
                    logger.info("storeHealthData request log: File created " + file.getName());
                } else {
                    System.out.println("File "+file.getName()+" already exists.");
                    logger.info("storeHealthData request log: File "+file.getName()+" already exists.");
                }
                FileWriter myWriter = new FileWriter(file.getPath());
                myWriter.write(decrypted);
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
                logger.info("storeHealthData request log: Successfully wrote to the file");
            } catch (IOException e) {
                System.out.println("An error occurred while creating file.");
                logger.info("storeHealthData request log: An error occurred while creating file");
                e.printStackTrace();
            }
        } else {
            JSONObject response = new JSONObject("{\"status\":\"error\",\"message\":\"citizen not enrolled in given study\"}");
            logger.info("storeHealthData request log: Error citizen identified by "+citizenPseudo+" not enrolled in the study:"+studyId);
            return response;
        }

        JSONObject response = new JSONObject("{\"status\":\"ok\",\"message\":\"health data have been correctly stored\"}");
        logger.info("storeHealthData request log: data from citizen identified by "+citizenPseudo+" has been stored properly for the study:"+studyId);
        return response;
    }



    public static void addParticipant(String sehrAppId, String citizenPseudo, String studyId, String tmp){
        Participant p = new Participant(citizenPseudo, sehrAppId, studyId, tmp);
        dbManager.addParticipant(p);
    }

    public static boolean participantEnrolledInStudy(String citizenPseudo, String studyId){
        for(Participant p : dbManager.getAllParticipants()){
            if(p.getPseudo().equals(citizenPseudo) && p.getStudyId().equals(studyId)){
                logger.info("Citizen having pseudoId:"+citizenPseudo+" already enrolled for studyID:"+studyId);
                System.out.println("Citizen having pseudoId:"+citizenPseudo+" already enrolled for studyID:"+studyId);
                return true;
            }
        }
        return false;
    }

    public static String getParticipants(String studyID, String apikey) throws JsonProcessingException {
        if(!apikey.equals("CHURC1")){
            return "{\"status\":\"access denied\"}";
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dbManager.getParticipants(studyID));
    }

    public static String sendExitNotification(String citizenPseudo, String studyId, String reasonText) {
        logger.info("sendExitNotification request for studyID:"+studyId+" for pseudoId:"+citizenPseudo+" - For the reason:"+reasonText);
        dbManager.removeParticipantByPseudoAndStudy(citizenPseudo, studyId);;
        return "Participant " + citizenPseudo + " not involved anymore in research studies";
    }

    public static JSONObject getHealthDataByStudy(String studyID, String participant, String apikey) throws JSONException, IOException {

        if(!apikey.equals("CHURC1")){
            JSONObject response = new JSONObject("{\"status\":\"access denied\"}");
            return response;
        }

        JSONObject response = new JSONObject("{}");
        JSONArray healthData = new JSONArray();
        response.put("study", ""+studyID);

        File studyDir = new File("./research-health-data/"+studyID+"/"+participant);

        if(isNull(studyDir.listFiles())){
            logger.info("getHealthDataByStudy request for studyID:"+studyID+" and participant:"+participant+" - Error no data are stored for the current study or participant");
            return response;
        }
        //File[] participantDirs = studyDir.listFiles();
        //System.out.println("TEST:"+participantDirs.length);

        //for (File pDir : participantDirs){
        File [] dataFiles = studyDir.listFiles();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
        try {
            Date dmax = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss").parse("2000.01.01.01:01:01");

             for (File file : dataFiles) {
                 if(file.isFile()) {

                     Date fileDate = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss").parse(file.getName());
                     if(fileDate.compareTo(dmax) >= 0){
                         dmax = fileDate;
                     }
                 }
             }

            BufferedReader inputStream = null;
            String line;
            String StringFileContent = "";
            try {
                inputStream = new BufferedReader(new FileReader(studyDir.getAbsolutePath()+"/"+formatter.format(dmax)+".json"));
                while ((line = inputStream.readLine()) != null) {
                    //System.out.println(line);
                    StringFileContent = StringFileContent + line;
                }
            }catch(IOException e) {
                System.out.println(e);
            }
            finally {
                if (inputStream != null) {
                    inputStream.close();
                    healthData.put(new JSONObject(StringFileContent));
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }


         //}
        response.put("data", healthData);
        return response;
    }

}
