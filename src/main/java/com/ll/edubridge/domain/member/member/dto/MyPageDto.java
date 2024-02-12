package com.ll.edubridge.domain.member.member.dto;

import com.ll.edubridge.domain.course.course.dto.CourseDto;
import com.ll.edubridge.domain.member.member.entity.Member;
import lombok.Getter;

import java.util.List;


@Getter
public class MyPageDto {


    private List<CourseDto> learningCourses;

    private List<CourseDto> favoriteCourses;

    private int dailyAchievement;

    private int dailyGoal;

    public MyPageDto() {
    }

    public MyPageDto(List<CourseDto> learningCourses, List<CourseDto> favoriteCourses, Member member) {
        this.learningCourses = learningCourses;
        this.favoriteCourses = favoriteCourses;
        this.dailyAchievement = member.getDailyAchievement();
        this.dailyGoal = member.getDailyGoal();

    }

}
