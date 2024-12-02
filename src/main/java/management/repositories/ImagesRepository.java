package management.repositories;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import management.entities.users.UserDB;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<ImageDB, UUID> {

  List<ImageDB> getByStatus(ImageStatus status, Pageable pageable);

  @Query("SELECT i FROM ImageDB i " +
      "WHERE (i.status = :status1 OR i.status = :status2) " +
      "AND (i.taggerUser IS NULL OR i.taggerUser <> :taggerUser) " +
      "AND i.startHandled < :startHandled " +
      "ORDER BY i.startHandled ASC, i.status DESC")
  List<ImageDB> getNextTask(
      @Param("status1") ImageStatus status1,
      @Param("status2") ImageStatus status2,
      @Param("taggerUser") UserDB taggerUser,
      @Param("startHandled") Timestamp startHandled,
      Pageable pageable
  );
}
