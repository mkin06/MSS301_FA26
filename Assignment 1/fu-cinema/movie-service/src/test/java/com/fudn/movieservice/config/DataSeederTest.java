package com.fudn.movieservice.config;

import com.fudn.movieservice.repository.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class DataSeederTest {
    private final GenreRepository genres = mock(GenreRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final MovieRepository movies = mock(MovieRepository.class);
    private final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    private final DataSeeder seeder = new DataSeeder(genres, rooms, movies, showtimes);

    @Test void seedsEachEmptyCollection() {
        seeder.run();
        verify(genres).saveAll(anyList());
        verify(rooms).saveAll(anyList());
        verify(movies).saveAll(anyList());
        verify(showtimes).saveAll(anyList());
    }
    @Test void restartDoesNotDuplicateExistingData() {
        when(genres.count()).thenReturn(5L);
        when(rooms.count()).thenReturn(4L);
        when(movies.count()).thenReturn(4L);
        when(showtimes.count()).thenReturn(5L);
        seeder.run();
        verify(genres, never()).saveAll(anyList());
        verify(rooms, never()).saveAll(anyList());
        verify(movies, never()).saveAll(anyList());
        verify(showtimes, never()).saveAll(anyList());
    }
    @Test void populatedGenresDoNotPreventSeedingEmptyRooms() {
        when(genres.count()).thenReturn(5L);
        when(movies.count()).thenReturn(4L);
        when(showtimes.count()).thenReturn(5L);
        seeder.run();
        verify(genres, never()).saveAll(anyList());
        verify(rooms).saveAll(anyList());
        verify(movies, never()).saveAll(anyList());
        verify(showtimes, never()).saveAll(anyList());
    }
}
