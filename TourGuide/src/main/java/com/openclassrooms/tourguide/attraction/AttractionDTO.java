package com.openclassrooms.tourguide.attraction;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;

@Data
public class AttractionDTO {

	private List<ContactAttractionDTO> contactAttraction;
	private double latUser;
	private double lonUser;

}
