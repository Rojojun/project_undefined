package com.finalproject.breeding.image.repository;

import com.finalproject.breeding.image.model.CommunityImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityImageRepository extends JpaRepository<CommunityImage, Long> {


    List<CommunityImage> findByCommunityId(Long communityId);
}
