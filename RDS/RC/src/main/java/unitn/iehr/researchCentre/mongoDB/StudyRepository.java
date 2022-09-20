package unitn.iehr.researchCentre.mongoDB;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyRepository extends MongoRepository<Study, String> {

    Optional<Study> findById(String studyId);
}
