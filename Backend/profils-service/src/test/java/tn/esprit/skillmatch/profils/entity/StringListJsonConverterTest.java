package tn.esprit.skillmatch.profils.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringListJsonConverterTest {

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void listRoundTrip() {
        List<String> values = List.of("Java", "SQL", "Spring Boot");

        String json = converter.convertToDatabaseColumn(values);

        assertEquals(values, converter.convertToEntityAttribute(json));
    }

    @Test
    void nullAndBlankColumnsBecomeEmptyLists() {
        assertEquals(List.of(), converter.convertToEntityAttribute(null));
        assertEquals(List.of(), converter.convertToEntityAttribute(""));
    }
}
