package management.repositories;

import java.util.List;
import java.util.UUID;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<ImageDB, UUID> {

  List<ImageDB> getByStatus(ImageStatus status, Pageable pageable);
}
