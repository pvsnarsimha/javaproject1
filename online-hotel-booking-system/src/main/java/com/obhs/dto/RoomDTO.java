package com.obhs.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RoomDTO {
	private Long id;
	private String type;
	private int numberOfRooms;
	private double pricePerNight;
	private LocalDateTime lastUpdated;
	private List<String> images = new ArrayList<>();
	// Getters and Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public int getNumberOfRooms() { return numberOfRooms; }
	public void setNumberOfRooms(int numberOfRooms) { this.numberOfRooms = numberOfRooms; }
	public double getPricePerNight() { return pricePerNight; }
	public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
	public List<String> getImages() { return images; }
	public void setImages(List<String> images) { this.images = images; }
	public LocalDateTime getLastUpdated() { return lastUpdated; }
	public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}