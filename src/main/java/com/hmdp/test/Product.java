//import org.springframework.data.annotation.Id;
//import org.springframework.data.annotation.Version;
//
//@Entity
//public class Product {
//    @Id
//    private Long id;
//
//    private String name;
//    private int stock;
//
//    @Version  // 使用@Version注解实现乐观锁
//    private int version;
//
//    // 减少库存的方法
//    public boolean reduceStock(int quantity) {
//        if (this.stock >= quantity) {
//            this.stock -= quantity;
//            return true;
//        }
//        return false;
//    }
//
//    // getters and setters
//    // ...
//}