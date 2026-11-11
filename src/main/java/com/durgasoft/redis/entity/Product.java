package com.durgasoft.redis.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Double price;

    /** Required by JPA when it creates an entity from a database row. */
    protected Product() { }

    /** Creates a new product before its database-generated identifier exists. */
    public Product(String productName, Double price) {
        this.productName = productName;
        this.price = price;
    }

    /** Creates a product with an identifier, which is convenient for tests and projections. */
    public Product(Long id, String productName, Double price) {
        this.id = id;
        this.productName = productName;
        this.price = price;
    }

    /** Returns the database-generated product identifier. */
    public Long getId() { return id; }
    /** Returns the product name. */
    public String getProductName() { return productName; }
    /** Returns the product price. */
    public Double getPrice() { return price; }
    /** Changes the product name before the entity is saved. */
    public void setProductName(String productName) { this.productName = productName; }
    /** Changes the product price before the entity is saved. */
    public void setPrice(Double price) { this.price = price; }
}
