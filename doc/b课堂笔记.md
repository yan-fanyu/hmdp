# 解决 使用 redis 中 set nx 命令实现分布式锁 四个缺点
使用 Redisson 中 封装的方法 解决 或者自己 使用 lua 脚本解决
![img_70.png](img_70.png)

# 实战篇-19 Redisson 可重入锁原理
锁的结构在 key 和 线程id 的基础上 加上重入的次数
使用 hash 结构

        filed       value
key     0x1231231     6

但是由于 hash 结构不像 string 结构那样 可以同时设置 nx 和 ex
所以要使用 lua 脚本 保证 判断锁是不是自己的 和 释放锁 的原子性

![img_69.png](img_69.png)

redisson 实际上底层代码 就是 用 lua 脚本实现的

#
