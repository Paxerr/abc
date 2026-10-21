package com.danasea.backend.modules.booking.infrastructure.persistence.entities;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class BookingItemJpaEntityMappingTest {

    @Test
    void mapsCapacityReleasedColumnForSchemaGeneration() throws NoSuchFieldException {
        Field field = BookingItemJpaEntity.class.getDeclaredField("capacityReleased");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("capacity_released");
        assertThat(column.nullable()).isFalse();
    }
}
