package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.example.datafairnessmodule.config.MainConfig.ThreadPoolSize;

public class ImproveExecutor {
    private ArrayList<ArrayList<Integer>> list;
    private Map<String, Object> map;
    private List<Improvement> improvements = new ArrayList<>();
    private MainRepository mainRepository;

    private final Executor executor = Executors.newFixedThreadPool(ThreadPoolSize, (Runnable r) -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public ImproveExecutor(ArrayList<ArrayList<Integer>> list, Map<String, Object> map, MainRepository mainRepository) {
        this.list = list;
        this.map = map;
        this.mainRepository = mainRepository;
        for (int i = 0; i < this.list.size(); i++) {
            Improvement improvement = new Improvement(i, list.get(i), map, mainRepository);
            this.improvements.add(improvement);
        }
    }

    public List<Map<String, Object>> getList() {
        List<CompletableFuture<Map<String, Object>>> improveFutures = improvements.stream()
                .map(improvement -> CompletableFuture.supplyAsync(() -> improvement.getMap(), executor))
                .collect(Collectors.toList());
        return improveFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

}
