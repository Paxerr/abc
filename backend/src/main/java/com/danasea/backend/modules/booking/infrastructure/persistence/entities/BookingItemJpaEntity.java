package com.danasea.backend.modules.booking.infrastructure.persistence.entities;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItemJpaEntity extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingJpaEntity booking;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "slot_id")
    private UUID slotId;

    @Column(name = "option_id")
    private UUID optionId;

    @Column(name = "pricing_unit")
    private String pricingUnit;

    @Column(name = "participants_count")
    private Integer participantsCount;

    @Column(name = "max_pax_per_package")
    private Integer maxPaxPerPackage;

    @Column(name = "allow_split", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean allowSplit = false;

    @Column(name = "capacity_released", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean capacityReleased = false;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_time", nullable = false)
    private LocalTime bookingTime;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @OneToMany(mappedBy = "bookingItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BookingItemAllocationJpaEntity> allocations = new ArrayList<>();

    public void addAllocation(BookingItemAllocationJpaEntity allocation) {
        if (allocations == null) {
            allocations = new ArrayList<>();
        }
        allocations.add(allocation);
        allocation.setBookingItem(this);
    }
}
