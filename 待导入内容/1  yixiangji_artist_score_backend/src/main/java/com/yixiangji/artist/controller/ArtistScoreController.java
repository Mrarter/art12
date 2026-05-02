package com.yixiangji.artist.controller;

import com.yixiangji.artist.entity.ArtistScore;
import com.yixiangji.artist.service.ArtistScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artist/score")
@RequiredArgsConstructor
public class ArtistScoreController {
    private final ArtistScoreService artistScoreService;

    @PostMapping("/recalculate/{artistId}")
    public ArtistScore recalculate(@PathVariable Long artistId) {
        return artistScoreService.recalculateScore(artistId);
    }

    @GetMapping("/{artistId}")
    public ArtistScore getScore(@PathVariable Long artistId) {
        return artistScoreService.getScore(artistId);
    }
}
