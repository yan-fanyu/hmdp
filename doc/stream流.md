# Java Stream 流

Stream 是 Java 8 引入的一个新特性，用于处理集合数据的函数式编程方式。它允许你以声明式的方式处理数据集合，可以非常高效地执行复杂的操作如过滤、映射、排序、归约等。

## 主要特点

1. **不是数据结构**：Stream 本身不存储数据，数据存储在底层集合或由生成器生成
2. **函数式编程**：支持 lambda 表达式和方法引用
3. **惰性执行**：中间操作是惰性的，只有终端操作才会触发实际计算
4. **只能消费一次**：Stream 一旦被消费就不能再次使用

## 创建 Stream

```java
// 从集合创建
List<String> list = Arrays.asList("a", "b", "c");
Stream<String> stream1 = list.stream();

// 从数组创建
String[] array = {"a", "b", "c"};
Stream<String> stream2 = Arrays.stream(array);

// 直接创建
Stream<String> stream3 = Stream.of("a", "b", "c");

// 创建无限流
Stream<Integer> infiniteStream = Stream.iterate(0, n -> n + 2);
```

## 常用操作

### 中间操作（返回 Stream）

- **filter(Predicate)**：过滤元素
- **map(Function)**：转换元素
- **flatMap(Function)**：将流中的每个元素转换为流，然后合并
- **distinct()**：去重
- **sorted()**：排序
- **peek(Consumer)**：查看元素但不修改流
- **limit(long)**：限制元素数量
- **skip(long)**：跳过前n个元素

### 终端操作（返回非 Stream 结果）

- **forEach(Consumer)**：遍历每个元素
- **toArray()**：转换为数组
- **reduce(BinaryOperator)**：归约操作
- **collect(Collector)**：转换为集合或其他形式
- **min()/max()**：最小/最大值
- **count()**：元素计数
- **anyMatch()/allMatch()/noneMatch()**：匹配检查
- **findFirst()/findAny()**：查找元素

## 示例

```java
List<String> strings = Arrays.asList("abc", "", "bc", "efg", "abcd","", "jkl");

// 过滤空字符串并计数
long count = strings.stream().filter(string -> string.isEmpty()).count();

// 过滤空字符串并收集到列表
List<String> filtered = strings.stream()
    .filter(string -> !string.isEmpty())
    .collect(Collectors.toList());

// 映射为字符串长度
List<Integer> lengths = strings.stream()
    .filter(s -> !s.isEmpty())
    .map(String::length)
    .collect(Collectors.toList());

// 并行处理
long parallelCount = strings.parallelStream()
    .filter(string -> string.isEmpty())
    .count();

// 归约操作
Optional<String> concatenated = strings.stream()
    .filter(s -> !s.isEmpty())
    .reduce((s1, s2) -> s1 + ", " + s2);
```

## 收集器 (Collectors)

Collectors 类提供了许多静态方法用于常见的归约操作：

```java
// 转换为List
List<String> list = stream.collect(Collectors.toList());

// 转换为Set
Set<String> set = stream.collect(Collectors.toSet());

// 转换为Map
Map<String, Integer> map = stream.collect(
    Collectors.toMap(s -> s, String::length));

// 连接字符串
String joined = stream.collect(Collectors.joining(", "));

// 分组
Map<Integer, List<String>> groups = stream.collect(
    Collectors.groupingBy(String::length));

// 分区
Map<Boolean, List<String>> partition = stream.collect(
    Collectors.partitioningBy(s -> s.length() > 3));
```

Stream API 提供了一种高效且易读的方式来处理集合数据，特别适合大数据量的处理。