package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Comparator;
import java.util.HashMap;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;

import com.openclassrooms.tourguide.attraction.ContactAttractionDTO;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;  //convertir les miles nautiques en miles terrestre

	// proximity in miles
	private int defaultProximityBuffer = 10; //proximiter d'un user proche d'une attraction 
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200; //proximiter d'un user n'est plus proche d'une attraction
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executorService = Executors.newFixedThreadPool(100);

	/**
	 * On prend l'emplacement d'un user avec GpsUtil
	 * On récupére les 5 attractions les plus proche
	 */
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	/*
	 * calcule récompense pour un user
	 * si il n'a pas deja la recompense, et si il est proche la récompense est ajouter à l'user
	 */
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for(VisitedLocation visitedLocation : userLocations) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				for(Attraction attraction : attractions) {
					if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
						if(nearAttraction(visitedLocation, attraction)) {
							user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
						}
					}
				}
			},executorService);
			futures.add(future);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
	}


	public List<ContactAttractionDTO> sortAttractionsAndRewards (VisitedLocation visitedLocation, List<Attraction> attractionsList,User user ){
		List<ContactAttractionDTO> contactAttractionDTO = new ArrayList<>();
		Map<Attraction, Double> distancesMap = new HashMap<>();

		for(Attraction attraction : attractionsList) {
			double distance = getDistance(visitedLocation.location,attraction);
			distancesMap.put(attraction, distance);
		}    
		attractionsList.sort(Comparator.comparingDouble(distancesMap::get));

		List<Attraction> fiveFirstAttractions = attractionsList.subList(0, 5);
		for (Attraction attractionReward : fiveFirstAttractions) {
			ContactAttractionDTO attractionWithRewardDTO = new ContactAttractionDTO();
			int rewardPointByAttraction = getRewardPoints(attractionReward, user);
			attractionWithRewardDTO.setLatAttraction(attractionReward.latitude);
			attractionWithRewardDTO.setLonAttraction(attractionReward.longitude);
			attractionWithRewardDTO.setNameAttraction(attractionReward.attractionName);
			attractionWithRewardDTO.setRewardAttraction(rewardPointByAttraction);
			attractionWithRewardDTO.setDistance(getDistance(visitedLocation.location, attractionReward));
			contactAttractionDTO.add(attractionWithRewardDTO);
		}
		return contactAttractionDTO;
	}

	//	Collections.sort(attraction, new Comparator<Attraction>() {
	//	public int compare(Attraction a1, Attraction a2) {
	//		double distance1 = getDistance(visitedLocation.location, attraction);
	//		double distance2 = getDistance(visitedLocation.location, attraction);
	//		return Double.compare(distance1, distance2);
	//	}
	//});





	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	/*
	 * calcule en miles la distance entre deux points donnée par des longitude et latitude
	 */
	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

}
