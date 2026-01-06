package com.paul.csvpipeline.backend.csvupload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CsvUploadRepository extends JpaRepository<CsvUpload, Long> {

    List<CsvUpload> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<CsvUpload> findByIdAndUserEmail(Long id, String userEmail);
}
