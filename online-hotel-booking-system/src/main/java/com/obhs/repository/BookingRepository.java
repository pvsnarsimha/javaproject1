package com.obhs.repository;

import com.obhs.entity.Booking;
import com.obhs.entity.Room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);

    @Query("SELECT b FROM Booking b WHERE b.room = :room AND " +
           "(b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)")
    List<Booking> findByRoomAndDateRange(@Param("room") Room room,
                                        @Param("checkInDate") LocalDate checkInDate,
                                        @Param("checkOutDate") LocalDate checkOutDate);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId")
    List<Booking> findByRoomId(@Param("roomId") Long roomId);

    @Query(value = "SELECT COUNT(*) FROM bookings WHERE room_id = :roomId", nativeQuery = true)
    Long countBookingsByRoomIdNative(@Param("roomId") Long roomId);

    @Modifying
    @Query("DELETE FROM Booking b WHERE b.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);

    @Modifying
    @Query(value = "DELETE FROM bookings WHERE room_id = :roomId", nativeQuery = true)
    void deleteByRoomIdNative(@Param("roomId") Long roomId);
}