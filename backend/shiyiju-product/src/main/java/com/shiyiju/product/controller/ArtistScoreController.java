package com.shiyiju.product.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.entity.ArtistScore;
import com.shiyiju.product.service.ArtistScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artist/score")
@RequiredArgsConstructor
public class ArtistScoreController {

    private final ArtistScoreService artistScoreService;

    @GetMapping("/{artistId}")
    public Result<ArtistScore> getScore(@PathVariable Long artistId) {
        ArtistScore score = artistScoreService.getScore(artistId);
        return Result.success(score);
    }

    @PostMapping("/recalculate/{artistId}")
    public Result<ArtistScore> recalculate(@PathVariable Long artistId) {
        ArtistScore score = artistScoreService.recalculateScore(artistId);
        return Result.success(score);
    }
}
