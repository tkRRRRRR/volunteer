package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.dto.FavoriteRequest;
import com.example.volunteer.entity.Favorite;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.FavoriteMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.FavoriteService;
import com.example.volunteer.vo.FavoriteVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;

    public FavoriteServiceImpl(
            FavoriteMapper favoriteMapper,
            UniversityMapper universityMapper,
            MajorMapper majorMapper
    ) {
        this.favoriteMapper = favoriteMapper;
        this.universityMapper = universityMapper;
        this.majorMapper = majorMapper;
    }

    @Override
    public List<FavoriteVO> list(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getId));
        if (favorites.isEmpty()) {
            return List.of();
        }

        Set<Long> universityIds = favorites.stream()
                .map(Favorite::getUniversityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> majorIds = favorites.stream()
                .map(Favorite::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, University> universities = universityMapper.selectBatchIds(universityIds).stream()
                .collect(Collectors.toMap(University::getId, Function.identity()));
        Map<Long, Major> majors = majorIds.isEmpty()
                ? Map.of()
                : majorMapper.selectBatchIds(majorIds).stream()
                .collect(Collectors.toMap(Major::getId, Function.identity()));

        return favorites.stream()
                .map(favorite -> toVO(favorite, universities.get(favorite.getUniversityId()),
                        favorite.getMajorId() == null ? null : majors.get(favorite.getMajorId())))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public FavoriteVO add(Long userId, FavoriteRequest request) {
        University university = universityMapper.selectById(request.getUniversityId());
        if (university == null) {
            throw BusinessException.notFound("院校不存在");
        }

        Major major = null;
        if (request.getMajorId() != null) {
            major = majorMapper.selectById(request.getMajorId());
            if (major == null || !request.getUniversityId().equals(major.getUniversityId())) {
                throw BusinessException.badRequest("专业与院校不匹配");
            }
        }

        LambdaQueryWrapper<Favorite> duplicateQuery = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getUniversityId, request.getUniversityId());
        if (request.getMajorId() == null) {
            duplicateQuery.isNull(Favorite::getMajorId);
        } else {
            duplicateQuery.eq(Favorite::getMajorId, request.getMajorId());
        }
        if (favoriteMapper.selectCount(duplicateQuery) > 0) {
            throw BusinessException.badRequest("该院校专业已收藏");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setUniversityId(request.getUniversityId());
        favorite.setMajorId(request.getMajorId());
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return toVO(favorite, university, major);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long favoriteId) {
        Favorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null) {
            throw BusinessException.notFound("收藏记录不存在");
        }
        if (!userId.equals(favorite.getUserId())) {
            throw BusinessException.forbidden("无权删除该收藏");
        }
        favoriteMapper.deleteById(favoriteId);
    }

    private FavoriteVO toVO(Favorite favorite, University university, Major major) {
        if (university == null) {
            return null;
        }
        return new FavoriteVO(
                favorite.getId(),
                university.getId(),
                university.getName(),
                university.getProvince(),
                university.getCity(),
                university.getLevel(),
                major == null ? null : major.getId(),
                major == null ? "院校专业组" : major.getName()
        );
    }
}
