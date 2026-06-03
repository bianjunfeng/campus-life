package com.campus.campus_life_backend.modules.user.mapper;

import com.campus.campus_life_backend.modules.user.entity.StudentProfile;
import org.apache.ibatis.annotations.Param;

public interface StudentProfileMapper {

    int insertStudentProfile(StudentProfile studentProfile);

    /**
     * 根据用户ID查询已通过审核的学生档案（status=1�?
     */
    StudentProfile findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询学生档案（包括所有状态）
     */
    StudentProfile findByUserIdAllStatus(@Param("userId") Long userId);

    /**
     * 根据学校和学号查询已通过审核的学生档案（status=1�?
     */
    StudentProfile findBySchoolAndStudentNo(@Param("school") String school, @Param("studentNo") String studentNo);

    int updateStudentProfile(StudentProfile studentProfile);

    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
}


