package com.campus.campus_life_ai.knowledge.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentParseService {

    public String parse(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
