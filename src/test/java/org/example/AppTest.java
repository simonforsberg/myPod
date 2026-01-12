package org.example;

import org.example.repo.PlaylistRepositoryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    private PlaylistRepositoryImpl playlistRepo;

    @BeforeEach
    void setup() {
        playlistRepo = new PlaylistRepositoryImpl(TestPersistenceManager.get());
    }

    @AfterAll
    static void tearDown() {
        TestPersistenceManager.close();
    }

    @Test
    void test() {
        assertThat(true).isTrue();
    }
}
