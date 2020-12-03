package kr.co.promptech.profiler.reopository.meta;

import kr.co.promptech.profiler.model.meta.Meta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * meta에 대한 JpaRepository 클래스
 */
@Repository
public interface MetaRepository extends JpaRepository<Meta, Integer> {
    Meta findByTitle(String title);

    @Query(value = "select column_name from information_schema.columns where table_name =:title", nativeQuery = true)
    List<String> findAllColumns(@Param("title") String title);
}
