package unitn.iehr.researchCentre.mongoDB;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("participants")
public class Participant {

    @Id
    private String id;
    private String pseudo;
    private String sehrappId;
    private String studyId;
    private String timestamp;

    public Participant(String pseudo, String sehrappId, String studyId, String timestamp) {
        super();
        this.pseudo = pseudo;
        this.sehrappId = sehrappId;
        this.studyId = studyId;
        this.timestamp = timestamp;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getSehrappId() {
        return sehrappId;
    }

    public String getStudyId() {
        return studyId;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
