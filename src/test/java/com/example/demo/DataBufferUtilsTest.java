package com.example.demo;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

class DataBufferUtilsTest {

  @Test
  void read() {
    var factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    var resource = new ClassPathResource("test.txt");

    DataBufferUtils.read(resource, factory, 8192).blockLast();
  }
}
