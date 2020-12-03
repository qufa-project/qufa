package kr.co.promptech.profiler.service.meta;

import kr.co.promptech.profiler.model.meta.MetaColumn;
import kr.co.promptech.profiler.reopository.meta.MetaColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * meta_column에 대한 서비스를 담당하는 클래스
 */
@RequiredArgsConstructor
@Service
public class MetaColumnService {

    private final MetaColumnRepository metaColumnRepository;

    public List<String> getColumnsByTargetId(int id) {
        List<MetaColumn> metaColumns = metaColumnRepository.findAllByMetaId(id);

        List<String> columns = new ArrayList<>();
        metaColumns.forEach(metaColumn -> {
            if (!metaColumn.getOriginalColumnName().equals("id"))
                columns.add(metaColumn.getOriginalColumnName());
        });

        return columns;
    }

    public void save(MetaColumn metaColumn) {
        this.metaColumnRepository.save(metaColumn);
    }
}
