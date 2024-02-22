package com.openclassrooms.tourguide.attraction;

import lombok.Data;

@Data
public class ContactAttractionDTO {
	
	private double latAttraction;
	private double lonAttraction;
	private String nameAttraction;
	private Integer rewardAttraction;
	private double distance;

}
