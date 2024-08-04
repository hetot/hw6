package com.github.javarar.poke.service;

import com.github.javarar.poke.model.PokemonDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PokemonService {
    private final List<PokemonDto> pokemons = new ArrayList<>();

    public PokemonDto getByName(String name) {
        return pokemons.stream().filter(pokemon -> pokemon.getName().equals(name)).findFirst().orElse(null);
    }
}
