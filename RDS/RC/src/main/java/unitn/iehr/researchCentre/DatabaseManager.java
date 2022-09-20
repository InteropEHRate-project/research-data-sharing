package unitn.iehr.researchCentre;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;
import unitn.iehr.researchCentre.mongoDB.Participant;
import unitn.iehr.researchCentre.mongoDB.ParticipantRepository;
import java.util.List;

@EnableMongoRepositories({"unitn.iehr.researchCentre"})
@Component
public class DatabaseManager {

    @Autowired
    private ParticipantRepository participantRepo;

    public DatabaseManager(ParticipantRepository repo){
        this.participantRepo = repo;
    }

    public void addParticipant (Participant p){
        participantRepo.save(p);
    }

    public List<Participant> getParticipants (String studyID){
        //System.out.println(participantRepo.findByPseudo(studyID).getPseudo());
        return participantRepo.findByStudyId(studyID);
    }

    public List<Participant> getAllParticipants (){
        //System.out.println(participantRepo.findByPseudo(studyID).getPseudo());
        return participantRepo.findAll();
    }

    public void removeParticipantByPseudo (String citizenPseudo){
        participantRepo.deleteParticipantByPseudo(citizenPseudo);
    }

    public void removeParticipantByPseudoAndStudy (String citizenPseudo, String studyId){
        participantRepo.deleteParticipantByPseudoAndStudyId(citizenPseudo, studyId);
    }

}
