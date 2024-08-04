package com.github.javarar.poke.model;

import lombok.Data;

import java.util.List;

@Data
public class PokemonDto {
    private String name;
    private int height;
    private int weight;
    private List<String> abilities;
}
