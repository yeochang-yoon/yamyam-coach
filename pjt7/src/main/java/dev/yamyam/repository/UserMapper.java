package dev.yamyam.repository;

import dev.yamyam.domain.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") String id);

    int insert(User user);

    int updateProfile(User user);

    int delete(@Param("id") String id);
}
