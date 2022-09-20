package unitn.iehr.researchCentre.mongoDB;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("studies")
public class Study {
    @Id
    private String id;
    private String name;
    private String researcherId;

    public Study(String id, String name, String researcherId) {
        super();
        this.id = id;
        this.name = name;
        this.researcherId = researcherId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getResearcherId() {
        return researcherId;
    }
}
