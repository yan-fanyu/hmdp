package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOW_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1 获取登录用户
        Long userId = UserHolder.getUser().getId();

        String key = FOLLOW_KEY + userId;
        // 1 判断是关注还是取关
        if(isFollow){
            // 2 关注 新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注用户的id 放入 redis 的 set 集合   --> sadd userId followerUserIds
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        }else{
            // 3 取关 删除数据    delete from tb_follow where user_id = ? and follow_user_id = ?
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if(isSuccess){
                // 把关注用户的 id 从 Redis 集合中移出
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }

        return Result.ok();
    }

//    @Override
//    public Result isFollow(Long followUserId) {
//        Long id = UserHolder.getUser().getId();
//        Integer count = query().eq("user_id", id).eq("follow_user_id", followUserId).count();
//        return Result.ok(count > 0);
//    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

//    @Override
//    public Result isFollow(Long followUserId) {
//        // 1 获取登录用户
//        Long userId = UserHolder.getUser().getId();
//        // 2 查询是否关注 select count(*) tb_follow where user_id = ? and follow_user_id = ?
//        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
//        return Result.ok(count > 0);
//    }

    @Override
    public Result followCommons(Long id) {
        // 1 获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + userId;

        // 2 求交集
        String key2 = FOLLOW_KEY + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);

        if(intersect == null || intersect.isEmpty()){
            // 无交集
            return Result.ok(Collections.emptyList());
        }

        // todo: 面试 哪里用了 stream 流
        /*
        回答：
        比如在判断用户共同关注时候，首先获取到当前登录的用户id，然后获取到另一个用户的id
        在获取到这两个用户id 以后，先通过redis 调用 intersect 方法查找 两个用户关注列表的交集
        得到 Set<String> ids类型
        然后通过 ids.stream().map(Long::valueOf).collect(Collectors.toList())  得到共同关注好友的 id list

        然后再次利用stream 流
        先通过 mapper 访问数据库得到所有的用户 user
        调用 stream() 方法 然后 map 将 user 转化为 userDTO 转化为前端所需的数据

        最后 把 UserDTO 集合返回给前端
         */

        // 3 解析 id 集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4 查询用户
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
