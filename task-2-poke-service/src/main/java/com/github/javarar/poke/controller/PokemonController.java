package com.github.javarar.poke.controller;

import com.github.javarar.poke.model.PokemonDto;
import com.github.javarar.poke.service.PokemonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RestController("/api/v1/pokemon")
@RequiredArgsConstructor
public class PokemonController {
    private final PokemonService pokemonService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<UUID, List<Future<PokemonDto>>> pokemonMap = new ConcurrentHashMap<>();

    @PostMapping("/async/getTaskId")
    public ResponseEntity<UUID> getTaskId(@RequestBody List<String> names) {
        var taskId = UUID.randomUUID();
        List<Future<PokemonDto>> futures = new ArrayList<>();
        for (String name : names) {
            futures.add(executor.submit(() -> pokemonService.getByName(name)));
        }
        pokemonMap.put(taskId, futures);
        return ResponseEntity.ok(taskId);
    }

    @GetMapping("/async/{taskId}")
    public ResponseEntity<List<PokemonDto>> get(@PathVariable UUID taskId) {
        List<PokemonDto> pokemonDtos = new ArrayList<>();
        List<Future<PokemonDto>> futures = pokemonMap.get(taskId);
        for (var future : futures) {
            if (!future.isDone()) {
                return ResponseEntity.ok(null);
            }
            try {
                var tmp = future.get();
                if (tmp != null) {
                    pokemonDtos.add(tmp);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        pokemonMap.remove(taskId);
        return ResponseEntity.ok(pokemonDtos);
    }

    @PostMapping("/sync")
    public ResponseEntity<List<PokemonDto>> pokemonSync(@RequestBody List<String> names) {
        List<PokemonDto> result = new ArrayList<>();
        for (String name : names) {
            var tmp = pokemonService.getByName(name);
            if (tmp != null) {
                result.add(tmp);
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/oneOf")
    public ResponseEntity<PokemonDto> pokemonSyncOneOf(@RequestBody List<String> names) {
        for (String name : names) {
            var tmp = pokemonService.getByName(name);
            if (tmp != null) {
                return ResponseEntity.ok(tmp);
            }
        }
        return ResponseEntity.ok(null);
    }

    @PostMapping("/sync/parallel")
    public ResponseEntity<List<PokemonDto>> pokemonSyncParallel(@RequestBody List<String> names) {
        List<PokemonDto> result = new ArrayList<>();
        List<Future<PokemonDto>> futures = new ArrayList<>();
        for (String name : names) {
            futures.add(executor.submit(() -> pokemonService.getByName(name)));
        }
        for (var future : futures) {
            try {
                var tmp = future.get();
                if (tmp != null) {
                    result.add(tmp);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/parallel/oneOf")
    public ResponseEntity<PokemonDto> pokemonSyncParallelOneOf(@RequestBody List<String> names) {
        List<Future<PokemonDto>> futures = new ArrayList<>();
        for (String name : names) {
            futures.add(executor.submit(() -> pokemonService.getByName(name)));
        }
        while (!futures.isEmpty()) {
            List<Future<PokemonDto>> markDeleted = new ArrayList<>();
            for (var future : futures) {
                if (future.isDone()) {
                    markDeleted.add(future);
                    try {
                        var tmp = future.get();
                        if (tmp != null) {
                            return ResponseEntity.ok(tmp);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            futures.removeAll(markDeleted);
        }
        return ResponseEntity.ok(null);
    }
}
