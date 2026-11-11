package com.durgasoft.redis.dto;

import java.io.Serializable;

public record ProductResponse(Long id, String productName, Double price) implements Serializable { }
