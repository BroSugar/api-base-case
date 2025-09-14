package com.example.service;

import com.example.entity.User;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "users")
public class UserService {
    // 模拟数据库查询
    @Cacheable(key = "#id")
    public User getUserById(Long id) {
        System.out.println("🔍 [数据库] 查询用户 ID: " + id);
        return new User(id, "User-" + id);
    }

    @CachePut(key = "#user.id")
    public User updateUser(User user) {
        System.out.println("💾 [数据库] 更新用户: " + user);
        return user;
    }

    @CacheEvict(key = "#id")
    public void deleteUser(Long id) {
        System.out.println("🗑️ [数据库] 删除用户 ID: " + id);
    }
}
