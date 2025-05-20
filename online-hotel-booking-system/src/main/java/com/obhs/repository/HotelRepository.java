package com.obhs.repository;

import com.obhs.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    @Modifying
    @Query(value = "DELETE FROM hotel_amenities WHERE hotel_id = :hotelId", nativeQuery = true)
    void deleteAmenitiesByHotelId(@Param("hotelId") Long hotelId);

    @Modifying
    @Query(value = "DELETE FROM hotel_images WHERE hotel_id = :hotelId", nativeQuery = true)
    void deleteImagesByHotelId(@Param("hotelId") Long hotelId);

    @Modifying
    @Query(value = "DELETE FROM rooms WHERE hotel_id = :hotelId", nativeQuery = true)
    void deleteRoomsByHotelId(@Param("hotelId") Long hotelId);
}