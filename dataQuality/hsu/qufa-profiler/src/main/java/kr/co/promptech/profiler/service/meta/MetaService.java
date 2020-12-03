package kr.co.promptech.profiler.service.meta;

import kr.co.promptech.profiler.model.meta.Meta;
import kr.co.promptech.profiler.reopository.meta.MetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * meta에 대한 서비스를 담당하는 클래스
 */
@RequiredArgsConstructor
@Service
public class MetaService {

    private final MetaRepository metaRepository;

    public int getIdByTitle(String title) {
        Meta meta = metaRepository.findByTitle(title);

        return meta.getId();
    }


    public void save(Meta meta) {
        metaRepository.save(meta);
    }

    public List<String> getColumnsByTitle(String title) {
        List<String> cols = metaRepository.findAllColumns(title);

        return cols;
    }

}
