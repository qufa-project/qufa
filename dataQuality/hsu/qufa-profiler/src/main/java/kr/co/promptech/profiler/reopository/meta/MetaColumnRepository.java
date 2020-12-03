package kr.co.promptech.profiler.reopository.meta;

import kr.co.promptech.profiler.model.meta.MetaColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * meta_column에 대한 JpaRepository 클래스
 */
@Repository
public interface MetaColumnRepository extends JpaRepository<MetaColumn, Integer> {
    List<MetaColumn> findAllByMetaId(int id);

}
