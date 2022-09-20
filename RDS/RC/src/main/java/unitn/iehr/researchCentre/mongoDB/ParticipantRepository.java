package unitn.iehr.researchCentre.mongoDB;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParticipantRepository extends MongoRepository<Participant, String> {

    List<Participant> findByStudyId(String studyId);
    void deleteParticipantByPseudo(String pseudo);
    void deleteParticipantByPseudoAndStudyId(String pseudo, String studyId);
}